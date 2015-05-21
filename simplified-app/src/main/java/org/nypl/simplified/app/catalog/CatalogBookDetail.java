package org.nypl.simplified.app.catalog;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.nypl.simplified.app.R;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSCategory;

import android.content.res.Resources;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Common functions used to configure book detail elements.
 */

final class CatalogBookDetail
{
  private static final URI    GENRES_URI;
  private static final String GENRES_URI_TEXT;

  static {
    GENRES_URI =
      NullCheck.notNull(URI
        .create("http://librarysimplified.org/terms/genres/Simplified/"));
    GENRES_URI_TEXT =
      NullCheck.notNull(CatalogBookDetail.GENRES_URI.toString());
  }

  static void configureSummaryPublisher(
    final OPDSAcquisitionFeedEntry e,
    final TextView summary_publisher)
  {
    final OptionType<String> pub = e.getPublisher();
    if (pub.isSome()) {
      final Some<String> some = (Some<String>) pub;
      summary_publisher.setText(some.get());
    }
  }

  static void configureSummaryWebView(
    final OPDSAcquisitionFeedEntry e,
    final WebView summary_text)
  {
    final StringBuilder text = new StringBuilder();
    text.append("<html>");
    text.append("<head>");
    text.append("<style>body {");
    text.append("padding: 0;");
    text.append("padding-right: 2em;");
    text.append("margin: 0;");
    text.append("}</style>");
    text.append("</head>");
    text.append("<body>");
    text.append(e.getSummary());
    text.append("</body>");
    text.append("</html>");

    final WebSettings summary_text_settings = summary_text.getSettings();
    summary_text_settings.setAllowContentAccess(false);
    summary_text_settings.setAllowFileAccess(false);
    summary_text_settings.setAllowFileAccessFromFileURLs(false);
    summary_text_settings.setAllowUniversalAccessFromFileURLs(false);
    summary_text_settings.setBlockNetworkLoads(true);
    summary_text_settings.setBlockNetworkImage(true);
    summary_text_settings.setDefaultTextEncodingName("UTF-8");
    summary_text_settings.setDefaultFixedFontSize(12);
    summary_text_settings.setDefaultFontSize(12);
    summary_text.loadDataWithBaseURL(
      null,
      text.toString(),
      "text/html",
      "UTF-8",
      null);
  }

  static void configureViewTextAuthor(
    final OPDSAcquisitionFeedEntry e,
    final TextView authors)
  {
    final StringBuilder buffer = new StringBuilder();
    final List<String> as = e.getAuthors();
    for (int index = 0; index < as.size(); ++index) {
      final String a = NullCheck.notNull(as.get(index));
      if (index > 0) {
        buffer.append("\n");
      }
      buffer.append(a);
    }
    authors.setText(NullCheck.notNull(buffer.toString()));
  }

  static void configureViewTextMeta(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final TextView meta)
  {
    final StringBuilder buffer = new StringBuilder();
    CatalogBookDetail.createViewTextPublicationDate(rr, e, buffer);
    CatalogBookDetail.createViewTextPublisher(rr, e, buffer);
    CatalogBookDetail.createViewTextCategories(rr, e, buffer);
    meta.setText(NullCheck.notNull(buffer.toString()));
  }

  static void createViewTextCategories(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final StringBuilder buffer)
  {
    final List<OPDSCategory> cats = e.getCategories();

    boolean has_genres = false;
    for (int index = 0; index < cats.size(); ++index) {
      final OPDSCategory c = NullCheck.notNull(cats.get(index));
      if (CatalogBookDetail.GENRES_URI_TEXT.equals(c.getScheme())) {
        has_genres = true;
      }
    }

    if (has_genres) {
      if (buffer.length() > 0) {
        buffer.append("\n");
      }

      buffer.append(NullCheck.notNull(rr
        .getString(R.string.catalog_categories)));
      buffer.append(": ");

      for (int index = 0; index < cats.size(); ++index) {
        final OPDSCategory c = NullCheck.notNull(cats.get(index));
        if (CatalogBookDetail.GENRES_URI_TEXT.equals(c.getScheme())) {
          buffer.append(c.getTerm());
          if ((index + 1) <= cats.size()) {
            buffer.append(", ");
          }
        }
      }
    }
  }

  static String createViewTextPublicationDate(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final StringBuilder buffer)
  {
    if (buffer.length() > 0) {
      buffer.append("\n");
    }

    final OptionType<Calendar> p_opt = e.getPublished();
    if (p_opt.isSome()) {
      final Some<Calendar> some = (Some<Calendar>) p_opt;
      final Calendar p = some.get();
      final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
      buffer.append(NullCheck.notNull(rr
        .getString(R.string.catalog_publication_date)));
      buffer.append(": ");
      buffer.append(fmt.format(p.getTime()));
      return NullCheck.notNull(buffer.toString());
    }

    return "";
  }

  static void createViewTextPublisher(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final StringBuilder buffer)
  {
    final OptionType<String> pub = e.getPublisher();
    if (pub.isSome()) {
      final Some<String> some = (Some<String>) pub;

      if (buffer.length() > 0) {
        buffer.append("\n");
      }

      buffer.append(NullCheck.notNull(rr
        .getString(R.string.catalog_publisher)));
      buffer.append(": ");
      buffer.append(some.get());
    }
  }

  private CatalogBookDetail()
  {
    throw new UnreachableCodeException();
  }
}