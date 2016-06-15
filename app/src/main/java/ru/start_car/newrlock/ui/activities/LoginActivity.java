package ru.start_car.newrlock.ui.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ru.start_car.newrlock.R;
import ru.start_car.newrlock.common.aids.EventHandler;

/**
 * Created by beerko on 12.06.16.
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    private final String login_TEXT = "ru.start_car.login";
    private final String password_TEXT = "ru.start_car.password";

    private static final String ip = "192.168.43.23";
    private static final int port = 40004;
    private static final Handler handler = new Handler();

    EditText loginText;
    EditText passwordText;
    Button button;
    CoordinatorLayout mRoot;
    SharedPreferences preferences;
    ProgressDialog dialog;

    /**
     * Callback on authentication is completed
     */
    private class AuthenticationCompletedEvent implements EventHandler {
        @Override
        public void invoke(final Object arg) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Boolean authOK = (Boolean) arg;
                       if  (authOK)  onLoginSuccess(); else onLoginFailed();
                    }catch (Exception e){

                    }

                }
            });
        }
    }

    /**
     * Callback on disconnect event happened
     */
    private class DisconnectedEvent implements EventHandler {
        @Override
        public void invoke(final Object arg) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        onLoginFailed();
                    } catch(Exception e) { }
                }
            });
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Activity onCreate");
        setContentView(R.layout.activity_login);
        loginText = (EditText) findViewById(R.id.et_login);
        passwordText = (EditText) findViewById(R.id.et_password);
        button = (Button) findViewById(R.id.btn_login);
        mRoot = (CoordinatorLayout) findViewById(R.id.coord);
        dialog = new ProgressDialog(LoginActivity.this);


        preferences = getPreferences(MODE_PRIVATE);
        loginText.setText(preferences.getString(login_TEXT, ""));
        passwordText.setText(preferences.getString(password_TEXT, ""));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    public void login() {

        if (!validate()) {
            onLoginFailed();
            return;
        }

        Log.d(TAG, "Login process starting");
        button.setEnabled(false);

        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage(getString(R.string.text_auth));
        dialog.show();

        // Код аутификации
        String login = loginText.getText().toString();
        char[] password = passwordText.getText().toString().toCharArray();


        InetAddress address = null;
        try {
            address = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        ConnectionSingletone.getInstance().setAuthenticationCompletedEventHandler(new AuthenticationCompletedEvent());
        ConnectionSingletone.getInstance().setDisconnectedEventHandler(new DisconnectedEvent());
        ConnectionSingletone.getInstance().start(address, port, login, password, false);
    }

    @Override
    public void onBackPressed() {
        // Запрет возврата в MainActivity
        moveTaskToBack(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    public void onLoginSuccess() {
        dialog.dismiss();
        button.setEnabled(true);
        preferences.edit().putString(password_TEXT, passwordText.getText().toString()).apply();
        preferences.edit().putString(login_TEXT, loginText.getText().toString()).apply();
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    public boolean validate() {
        boolean valid = true;

        String login = loginText.getText().toString();
        String password = passwordText.getText().toString();

        if (login.isEmpty()) {
            loginText.setError("enter a valid login");
            valid = false;
        } else {
            loginText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            passwordText.setError(null);
        }

        return valid;
    }

    public void onLoginFailed() {

        dialog.dismiss();
        ConnectionSingletone.getInstance().stop();
        Snackbar snackbar = Snackbar.make(mRoot, "Login failed", Snackbar.LENGTH_LONG);
        snackbar.show();
        button.setEnabled(true);
    }
}
