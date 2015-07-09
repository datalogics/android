package org.nypl.simplified.downloader.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

/**
 * The default implementation of the {@link DownloaderType} interface.
 */

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class DownloaderHTTP implements
  DownloaderType
{
  private static final class Download implements
    Runnable,
    DownloadType,
    HTTPResultMatcherType<InputStream, Unit, IOException>
  {
    private final URI                      uri;
    private final AtomicBoolean            cancel;
    private final HTTPType                 http;
    private final OptionType<HTTPAuthType> auth;
    private final File                     file;
    private final DownloadListenerType     listener;
    private long                           total;
    private final Logger                   log;

    public Download(
      final long in_id,
      final File in_file,
      final OptionType<HTTPAuthType> in_auth,
      final URI in_uri,
      final HTTPType in_http,
      final DownloadListenerType in_listener)
    {
      this.auth = NullCheck.notNull(in_auth);
      this.file = NullCheck.notNull(in_file);
      this.uri = NullCheck.notNull(in_uri);
      this.http = NullCheck.notNull(in_http);
      NullCheck.notNull(in_listener);

      final String name =
        String.format("%s[%d]", DownloaderHTTP.class, in_id);
      this.log = NullCheck.notNull(LoggerFactory.getLogger(name));
      this.cancel = new AtomicBoolean(false);
      this.listener = new DownloadListenerType() {
        @Override public void onDownloadStarted(
          final DownloadType d,
          final long in_expected)
        {
          try {
            in_listener.onDownloadStarted(d, in_expected);
          } catch (final Exception x) {
            Download.this.log.error(
              "Ignoring exception: onDownloadStarted raised: ",
              x);
          }
        }

        @Override public void onDownloadFailed(
          final DownloadType d,
          final int in_status,
          final long in_running_total,
          final OptionType<Exception> in_exception)
        {
          try {
            in_listener.onDownloadFailed(
              d,
              in_status,
              in_running_total,
              in_exception);
          } catch (final Exception x) {
            Download.this.log.error(
              "Ignoring exception: onDownloadFailed raised: ",
              x);
          }
        }

        @Override public void onDownloadDataReceived(
          final DownloadType d,
          final long in_running_total,
          final long in_expected_total)
        {
          try {
            in_listener.onDownloadDataReceived(
              d,
              in_running_total,
              in_expected_total);
          } catch (final Exception x) {
            Download.this.log.error(
              "Ignoring exception: onDownloadDataReceived raised: ",
              x);
          }
        }

        @Override public void onDownloadCompleted(
          final DownloadType d,
          final File in_file)
        {
          try {
            in_listener.onDownloadCompleted(d, in_file);
          } catch (final Exception x) {
            Download.this.log.error(
              "Ignoring exception: onDownloadCompleted raised: ",
              x);
          }
        }

        @Override public void onDownloadCancelled(
          final DownloadType d)
        {
          try {
            in_listener.onDownloadCancelled(d);
          } catch (final Exception x) {
            Download.this.log.error(
              "Ignoring exception: onDownloadCancelled raised: ",
              x);
          }
        }
      };

      this.total = 0L;
    }

    @Override public void run()
    {
      try {
        final RedirectFollower rf =
          new RedirectFollower(this.log, this.http, this.auth, 5, this.uri, 0);

        this.log.debug(
          "starting download, uri {} to file {}",
          this.uri,
          this.file);

        final HTTPResultType<InputStream> r = rf.call();
        r.matchResult(this);
      } catch (final Exception e) {
        this.listener.onDownloadFailed(this, -1, this.total, Option.some(e));
        this.failed();
      }
    }

    @Override public Unit onHTTPError(
      final HTTPResultError<InputStream> e)
      throws IOException
    {
      this.log.error("http error: status {}", e.getStatus());

      final OptionType<Exception> none = Option.none();
      this.listener.onDownloadFailed(this, e.getStatus(), this.total, none);
      this.failed();
      return Unit.unit();
    }

    @Override public Unit onHTTPException(
      final HTTPResultException<InputStream> e)
      throws IOException
    {
      this.log.error("http error: ", e.getError());

      this.listener.onDownloadFailed(
        this,
        -1,
        this.total,
        Option.some(e.getError()));
      this.failed();
      return Unit.unit();
    }

    @Override public Unit onHTTPOK(
      final HTTPResultOKType<InputStream> e)
      throws IOException
    {
      this.log.debug("http ok: ", e.getStatus());
      final long expected = e.getContentLength();
      this.log.debug("expecting {} bytes", expected);

      this.listener.onDownloadStarted(this, expected);

      final OutputStream out = new FileOutputStream(this.file);
      try {
        final InputStream stream = e.getValue();
        try {
          final byte[] buffer = new byte[1024];

          while (this.cancel.get() == false) {
            final int r = stream.read(buffer);
            if (r == -1) {
              break;
            }
            this.total += r;
            out.write(buffer, 0, r);
            this.listener.onDownloadDataReceived(this, this.total, expected);
          }

          if (this.cancel.get()) {
            this.log.debug("download cancelled");
            this.listener.onDownloadCancelled(this);
          } else {
            if (this.total != expected) {
              this.log.error(
                "received {} bytes but expected {}",
                this.total,
                expected);
              final OptionType<Exception> none = Option.none();
              this.listener.onDownloadFailed(
                this,
                e.getStatus(),
                this.total,
                none);
              this.failed();
            } else {
              this.log.debug("download completed");
              this.listener.onDownloadCompleted(this, this.file);
            }
          }

        } finally {
          stream.close();
        }
        return Unit.unit();
      } finally {
        out.close();
      }
    }

    private void failed()
    {
      this.file.delete();
    }

    @Override public void cancel()
    {
      this.log.debug("cancelling download");
      this.cancel.set(true);
    }
  }

  private static final Logger   LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(DownloaderHTTP.class));
  }

  private final HTTPType        http;
  private final ExecutorService exec;
  private final File            directory;
  private final AtomicLong      id_pool;

  public static DownloaderType newDownloader(
    final ExecutorService in_exec,
    final File in_directory,
    final HTTPType in_http)
  {
    return new DownloaderHTTP(in_exec, in_directory, in_http);
  }

  private DownloaderHTTP(
    final ExecutorService in_exec,
    final File in_directory,
    final HTTPType in_http)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.directory = NullCheck.notNull(in_directory);
    this.http = NullCheck.notNull(in_http);
    this.id_pool = new AtomicLong(0);
  }

  @Override public DownloadType download(
    final URI in_uri,
    final OptionType<HTTPAuthType> in_auth,
    final DownloadListenerType in_listener)
  {
    NullCheck.notNull(in_uri);
    NullCheck.notNull(in_auth);
    NullCheck.notNull(in_listener);

    final long id = this.id_pool.incrementAndGet();
    final File file =
      new File(this.directory, String.format("%016d.data", id));

    DownloaderHTTP.LOG.debug("queued download {} for {}", file, in_uri);
    final Download d =
      new Download(id, file, in_auth, in_uri, this.http, in_listener);
    this.exec.execute(d);
    return d;
  }
}