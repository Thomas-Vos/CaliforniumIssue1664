package com.example.californiumissue1664;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.util.NetworkInterfacesUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final Executor executor = Executors.newSingleThreadExecutor();

    ConnectivityManager connectivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectivityManager = getSystemService(ConnectivityManager.class);

        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // Binds the current process to network. See documentation for more details:
                // https://developer.android.com/reference/android/net/ConnectivityManager#bindProcessToNetwork(android.net.Network)
                connectivityManager.bindProcessToNetwork(network);
                printNetworkInterfaces();
            }

            @Override
            public void onLost(@NonNull Network network) {
                // This will now print null because network was lost and bindProcessToNetwork was called.
                // For some reason it does not always print null, not sure why.
                printNetworkInterfaces();

//                // This should allow the app to connect to other networks (e.g. mobile data) again.
//                // (that is what I think this does)
//                connectivityManager.bindProcessToNetwork(null);
//
//                // Should no longer print null.
//                printNetworkInterfaces();
            }
        };

        connectivityManager.requestNetwork(
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build(),
                callback
        );
    }

    private void printNetworkInterfaces() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                System.out.println("Network interfaces: " + null);

                // As interfaces is now null, see what happens with Californium.
                executeCaliforniumRequest();
            } else {
                List<NetworkInterface> list = Collections.list(interfaces);
                System.out.println("Network interfaces: " + list);

            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void executeCaliforniumRequest() {

        executor.execute(() -> {
            // This HTTP request should print an exception.
            doHttpRequest();
            // This Californium request should print null.
            doCaliforniumRequest();

            // Clear binding so network requests are allowed again.
            connectivityManager.bindProcessToNetwork(null);

            // Confirm that the network interfaces are available again, they are in my case.
            printNetworkInterfaces();

            NetworkInterfacesUtil.clear();

            // Add a small delay otherwise HTTP network request fails. (I do not know why this is needed)
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // HTTP request should work again.
            doHttpRequest();

            // I would expect this Californium request to work again, but it does not for me.
            // It still prints null.
            doCaliforniumRequest();
        });
    }

    private void doCaliforniumRequest() {
        try {
//        String uri = "coap://californium.eclipseprojects.io:5683/";
            String uri = "coap://35.185.40.182:5683/"; // same as above url
            CoapClient client = new CoapClient();
            Request request = new Request(CoAP.Code.GET);
            request.setURI(uri);
            CoapResponse response = client.advanced(request);
            if (response == null) {
                System.out.println("Coap response: " + null);
            } else {
                System.out.println("Coap response size: " + response.getResponseText().length());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void doHttpRequest() {
        try {
            URL url = new URL("http://example.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String content = "", line;
            while ((line = rd.readLine()) != null) {
                content += line + "\n";
            }
            System.out.println("doHttpRequest result size: " + content.length());
        } catch (Exception exception) {
            System.out.println("doHttpRequest result: " + exception);
        }
    }
}
