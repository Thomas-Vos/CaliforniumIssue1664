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

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);

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
//        String uri = "coap://californium.eclipseprojects.io:5683/";
        String uri = "coap://35.185.40.182:5683/"; // same as above url

        executor.execute(() -> {
            try {
                CoapClient client = new CoapClient(uri);
                Request request = new Request(CoAP.Code.GET);
                request.setURI(uri);
                CoapResponse response = client.advanced(request);
                if (response == null) {
                    System.out.println("Coap response: " + null);
                } else {
                    System.out.println("Coap response: " + response.getResponseText());
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }
}
