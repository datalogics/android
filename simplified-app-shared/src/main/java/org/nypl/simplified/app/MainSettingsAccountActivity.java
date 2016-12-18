package org.nypl.simplified.app;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType;
import org.nypl.simplified.books.core.AccountLogoutListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountsDatabaseType;
import org.nypl.simplified.books.core.AuthenticationDocumentType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.multilibrary.Account;
import org.nypl.simplified.multilibrary.AccountsRegistry;
import org.slf4j.Logger;

/**
 * The activity displaying the settings for the application.
 */

public final class MainSettingsAccountActivity extends SimplifiedActivity implements
  AccountLogoutListenerType,
  AccountGetCachedCredentialsListenerType {
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainSettingsActivity.class);
  }

  private @Nullable TextView account_name_text;
  private @Nullable TextView account_subtitle_text;
  private @Nullable ImageView account_icon;

  private @Nullable TextView barcode_text;
  private @Nullable TextView pin_text;
  private @Nullable TableLayout table_with_code;

  private @Nullable Button login;

  private Account account;

  /**
   * Construct an activity.
   */

  public MainSettingsAccountActivity() {

  }

  @Override
  protected SimplifiedPart navigationDrawerGetPart() {
    return SimplifiedPart.PART_ACCOUNT;
  }

  @Override
  protected boolean navigationDrawerShouldShowIndicator() {
    return true;
  }

  @Override
  public void onAccountIsLoggedIn(
    final AccountCredentials creds) {
    MainSettingsAccountActivity.LOG.debug("account is logged in: {}", creds);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final Resources rr = NullCheck.notNull(this.getResources());


    final TableLayout in_table_with_code = NullCheck.notNull(this.table_with_code);
    final TextView in_account_name_text = NullCheck.notNull(this.account_name_text);
    final TextView in_account_subtitle_text = NullCheck.notNull(this.account_subtitle_text);
    final ImageView in_account_icon = NullCheck.notNull(this.account_icon);

    final TextView in_barcode_text = NullCheck.notNull(this.barcode_text);
    final TextView in_pin_text = NullCheck.notNull(this.pin_text);
    final Button in_login = NullCheck.notNull(this.login);

    UIThread.runOnUIThread(
      new Runnable() {
        @Override
        public void run() {

          in_table_with_code.setVisibility(View.VISIBLE);
          in_account_name_text.setText(MainSettingsAccountActivity.this.account.getName());
          in_account_subtitle_text.setText(MainSettingsAccountActivity.this.account.getSubtitle());
          if (MainSettingsAccountActivity.this.account.getId() == 0) {
            in_account_icon.setImageResource(R.drawable.account_logo_nypl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 1) {
            in_account_icon.setImageResource(R.drawable.account_logo_bpl);
          } else if (MainSettingsAccountActivity.this.account.getId() == 2) {
            in_account_icon.setImageResource(R.drawable.account_logo_instant);
          }

          in_barcode_text.setText(creds.getBarcode().toString());
          in_barcode_text.setContentDescription(creds.getBarcode().toString().replaceAll(".(?=.)", "$0,"));
          in_pin_text.setText(creds.getPin().toString());
          in_pin_text.setContentDescription(creds.getPin().toString().replaceAll(".(?=.)", "$0,"));

          in_login.setText(rr.getString(R.string.settings_log_out));
          in_login.setOnClickListener(
            new OnClickListener() {
              @Override
              public void onClick(
                final @Nullable View v) {
                final LogoutDialog d = LogoutDialog.newDialog();
                d.setOnConfirmListener(
                  new Runnable() {
                    @Override
                    public void run() {
                      books.accountLogout(creds, MainSettingsAccountActivity.this);
                    }
                  });
                final FragmentManager fm =
                  MainSettingsAccountActivity.this.getFragmentManager();
                d.show(fm, "logout-confirm");
              }
            });

        }
      });
  }

  @Override
  public void onAccountIsNotLoggedIn() {
    /*
    do nothing
     */
  }

  @Override
  public void onAccountLogoutFailure(
    final OptionType<Throwable> error,
    final String message) {
    MainSettingsAccountActivity.LOG.debug("onAccountLogoutFailure");
    LogUtilities.errorWithOptionalException(
      MainSettingsAccountActivity.LOG, message, error);

    final Resources rr = NullCheck.notNull(this.getResources());
    final MainSettingsAccountActivity ctx = this;
    UIThread.runOnUIThread(
      new Runnable() {
        @Override
        public void run() {
          final AlertDialog.Builder b = new AlertDialog.Builder(ctx);
          b.setNeutralButton("OK", null);
          b.setMessage(rr.getString(R.string.settings_logout_failed_server));
          b.setTitle(rr.getString(R.string.settings_logout_failed));
          b.setCancelable(true);

        }
      });
  }

  @Override
  public void onAccountLogoutFailureServerError(final int code) {
    MainSettingsAccountActivity.LOG.error(
      "onAccountLoginFailureServerError: {}", code);

    final Resources rr = NullCheck.notNull(this.getResources());
    final OptionType<Throwable> none = Option.none();
    this.onAccountLogoutFailure(
      none, rr.getString(R.string.settings_logout_failed_server));

  }

  @Override
  public void onAccountLogoutSuccess() {
    MainSettingsAccountActivity.LOG.debug("onAccountLogoutSuccess");
    this.onAccountIsNotLoggedIn();

    final Resources rr = NullCheck.notNull(this.getResources());
    final Context context = MainSettingsAccountActivity.this.getApplicationContext();
    final CharSequence text =
      NullCheck.notNull(rr.getString(R.string.settings_logout_succeeded));
    final int duration = Toast.LENGTH_SHORT;

    final TextView bt = NullCheck.notNull(this.barcode_text);
    final TextView pt = NullCheck.notNull(this.pin_text);

    UIThread.runOnUIThread(
      new Runnable() {
        @Override
        public void run() {

          bt.setVisibility(View.GONE);
          pt.setVisibility(View.GONE);

          final Toast toast = Toast.makeText(context, text, duration);
          toast.show();
          finish();
          overridePendingTransition(0, 0);
          startActivity(getIntent());
          overridePendingTransition(0, 0);

        }
      });


// logout clever

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

      CookieManager.getInstance().removeAllCookies(null);
      CookieManager.getInstance().flush();
    } else {
      final CookieSyncManager cookie_sync_manager = CookieSyncManager.createInstance(MainSettingsAccountActivity.this);
      cookie_sync_manager.startSync();
      final CookieManager cookie_manager = CookieManager.getInstance();
      cookie_manager.removeAllCookie();
      cookie_manager.removeSessionCookie();
      cookie_sync_manager.stopSync();
      cookie_sync_manager.sync();
    }


  }

  @Override
  protected void onActivityResult(final int request_code, final int result_code, final Intent data) {
    if (request_code == 1) {
      // Challenge completed, proceed with using cipher
      final CheckBox in_pin_reveal = NullCheck.notNull(
        (CheckBox) this.findViewById(R.id.settings_reveal_password));

      if (result_code == RESULT_OK) {

        final TextView in_pin_text =
          NullCheck.notNull((TextView) this.findViewById(R.id.settings_pin_text));

        in_pin_text.setTransformationMethod(
          HideReturnsTransformationMethod.getInstance());
        in_pin_reveal.setChecked(true);
      } else {
        // The user canceled or didn't complete the lock screen
        // operation. Go to error/cancellation flow.
        in_pin_reveal.setChecked(false);
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn) {
    final MenuItem item = NullCheck.notNull(item_mn);
    switch (item.getItemId()) {

      case android.R.id.home: {
        onBackPressed();
        return true;
      }

      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }


  @Override
  protected void onCreate(
    final @Nullable Bundle state) {
    super.onCreate(state);


    final Bundle extras = getIntent().getExtras();
    if (extras != null) {
      this.account = new AccountsRegistry(this).getAccount(extras.getInt("selected_account"));
    }
    else
    {
      this.account = Simplified.getCurrentAccount();
    }


    final ActionBar bar = this.getActionBar();
    if (android.os.Build.VERSION.SDK_INT < 21) {
      bar.setDisplayHomeAsUpEnabled(false);
      bar.setHomeButtonEnabled(true);
      bar.setIcon(R.drawable.ic_arrow_back);
    } else {
      bar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(false);
    }


    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final DocumentStoreType docs = app.getDocumentStore();

    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(R.layout.settings_account, content_area, false));
    content_area.addView(layout);
    content_area.requestLayout();

    final TextView in_barcode_label = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_barcode_label));

    final TextView in_barcode_text = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_barcode_text));

    final TextView in_pin_label = NullCheck.notNull(
      (TextView) this.findViewById(R.id.settings_pin_label));

    final TextView in_pin_text =
      NullCheck.notNull((TextView) this.findViewById(R.id.settings_pin_text));

    final CheckBox in_pin_reveal = NullCheck.notNull(
      (CheckBox) this.findViewById(R.id.settings_reveal_password));

    final Button in_login =
      NullCheck.notNull((Button) this.findViewById(R.id.settings_login));


    final TextView account_name = NullCheck.notNull(
      (TextView) this.findViewById(android.R.id.text1));
    final TextView account_subtitle = NullCheck.notNull(
      (TextView) this.findViewById(android.R.id.text2));

    final ImageView in_account_icon = NullCheck.notNull(
      (ImageView) this.findViewById(R.id.account_icon));

    in_pin_text.setTransformationMethod(
      PasswordTransformationMethod.getInstance());
    if (android.os.Build.VERSION.SDK_INT >= 21) {
      this.handle_pin_reveal(in_pin_text, in_pin_reveal);
    } else {
      in_pin_reveal.setVisibility(View.GONE);
    }

    /**
     * Get labels from the current authentication document.
     */

    final AuthenticationDocumentType auth_doc =
      docs.getAuthenticationDocument();
    in_barcode_label.setText(auth_doc.getLabelLoginUserID());
    in_pin_label.setText(auth_doc.getLabelLoginPassword());


    final TableLayout in_table_with_code =
      NullCheck.notNull((TableLayout) this.findViewById(R.id.settings_login_table_with_code));
    in_table_with_code.setVisibility(View.GONE);

    in_login.setOnClickListener(
      new OnClickListener() {
        @Override
        public void onClick(
          final @Nullable View v) {

          MainSettingsAccountActivity.this.onLoginWithBarcode();

        }
      });


    this.navigationDrawerSetActionBarTitle();

    this.account_name_text = account_name;
    this.account_subtitle_text = account_subtitle;
    this.account_icon = in_account_icon;
    this.barcode_text = in_barcode_text;
    this.pin_text = in_pin_text;
    this.login = in_login;
    this.table_with_code = in_table_with_code;

    this.getWindow().setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
  }

  /**
   *
   */
  public void onLoginWithBarcode() {


    final LoginListenerType login_listener = new LoginListenerType() {
      @Override
      public void onLoginAborted() {
        MainSettingsAccountActivity.LOG.trace("feed auth: aborted login");
//        listener.onAuthenticationNotProvided();
      }

      @Override
      public void onLoginFailure(
        final OptionType<Throwable> error,
        final String message) {
        LogUtilities.errorWithOptionalException(
          MainSettingsAccountActivity.LOG, "failed login", error);
//        listener.onAuthenticationError(error, message);
      }

      @Override
      public void onLoginSuccess(
        final AccountCredentials creds) {
        MainSettingsAccountActivity.LOG.trace(
          "feed auth: login supplied new credentials");
//        LoginActivity.this.openCatalog();

        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);

      }
    };


    final FragmentManager fm = this.getFragmentManager();
    UIThread.runOnUIThread(
      new Runnable() {
        @Override
        public void run() {
          final AccountBarcode barcode = new AccountBarcode("");
          final AccountPIN pin = new AccountPIN("");

          final LoginDialog df =
            LoginDialog.newDialog("Login required", barcode, pin);
          df.setLoginListener(login_listener);
          df.show(fm, "login-dialog");
        }
      });

  }

  @TargetApi(21)
  private void handle_pin_reveal(final TextView in_pin_text, final CheckBox in_pin_reveal) {
    /**
     * Add a listener that reveals/hides the password field.
     */
    in_pin_reveal.setOnCheckedChangeListener(
      new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(
          final CompoundButton view,
          final boolean checked) {
          if (checked) {
            final KeyguardManager keyguard_manager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (!keyguard_manager.isKeyguardSecure()) {
              // Show a message that the user hasn't set up a lock screen.
              Toast.makeText(MainSettingsAccountActivity.this, R.string.settings_screen_Lock_not_setup,
                Toast.LENGTH_LONG).show();
              in_pin_reveal.setChecked(false);
            } else {
              final Intent intent = keyguard_manager.createConfirmDeviceCredentialIntent(null, null);
              if (intent != null) {
                startActivityForResult(intent, 1);
              }
            }
          } else {
            in_pin_text.setTransformationMethod(
              PasswordTransformationMethod.getInstance());
          }
        }
      });
  }

  @Override
  protected void onResume() {
    super.onResume();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final Resources rr = NullCheck.notNull(this.getResources());
    final TableLayout in_table_with_code = NullCheck.notNull(this.table_with_code);
    final TextView in_account_name_text = NullCheck.notNull(this.account_name_text);
    final TextView in_account_subtitle_text = NullCheck.notNull(this.account_subtitle_text);
    final ImageView in_account_icon = NullCheck.notNull(this.account_icon);

    final TextView in_barcode_text = NullCheck.notNull(this.barcode_text);
    final TextView in_pin_text = NullCheck.notNull(this.pin_text);
    final Button in_login = NullCheck.notNull(this.login);

    in_account_name_text.setText(MainSettingsAccountActivity.this.account.getName());
    in_account_subtitle_text.setText(MainSettingsAccountActivity.this.account.getSubtitle());

    if (MainSettingsAccountActivity.this.account.getId() == 0) {
      in_account_icon.setImageResource(R.drawable.account_logo_nypl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 1) {
      in_account_icon.setImageResource(R.drawable.account_logo_bpl);
    } else if (MainSettingsAccountActivity.this.account.getId() == 2) {
      in_account_icon.setImageResource(R.drawable.account_logo_instant);
    }

    final AccountsDatabaseType accounts_database  = Simplified.getAccountsDatabase(this.account, this);
    if (accounts_database.accountGetCredentials().isSome()) {
      final AccountCredentials creds = ((Some<AccountCredentials>) accounts_database.accountGetCredentials()).get();

      UIThread.runOnUIThread(
        new Runnable() {
          @Override
          public void run() {

            in_table_with_code.setVisibility(View.VISIBLE);

            in_barcode_text.setText(creds.getBarcode().toString());
            in_barcode_text.setContentDescription(creds.getBarcode().toString().replaceAll(".(?=.)", "$0,"));
            in_pin_text.setText(creds.getPin().toString());
            in_pin_text.setContentDescription(creds.getPin().toString().replaceAll(".(?=.)", "$0,"));

            in_login.setText(rr.getString(R.string.settings_log_out));
            in_login.setOnClickListener(
              new OnClickListener() {
                @Override
                public void onClick(
                  final @Nullable View v) {
                  final LogoutDialog d = LogoutDialog.newDialog();
                  d.setOnConfirmListener(
                    new Runnable() {
                      @Override
                      public void run() {
                        books.accountLogout(creds, MainSettingsAccountActivity.this);
                      }
                    });
                  final FragmentManager fm =
                    MainSettingsAccountActivity.this.getFragmentManager();
                  d.show(fm, "logout-confirm");
                }
              });

          }
        });
    }

  }
}