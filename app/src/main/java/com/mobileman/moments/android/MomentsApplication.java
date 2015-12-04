/*******************************************************************************
 * Copyright 2015 MobileMan GmbH
 * www.mobileman.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.mobileman.moments.android;

import android.app.Application;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.mobileman.moments.android.backend.service.NetworkStateListener;
import com.parse.SaveCallback;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Created by MobileMan on 28/04/15.
 */
public class MomentsApplication extends Application{

    public enum BackEndpoint { Production, Crm, MpLocal }

    public static final BackEndpoint ENDPOINT  = BackEndpoint.Production;



    public static boolean isOnProduction() {
        return (ENDPOINT == BackEndpoint.Production);
    }

    public static boolean isOnDevServer() {
        return (ENDPOINT == BackEndpoint.Crm);
    }

    private static MomentsApplication APPLICATION;

    public MomentsApplication() {
        super();
        APPLICATION = this;
    }

    private String serverUrl = null;

    public static MomentsApplication getApplication() {
        return APPLICATION;
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        Log.i(Util.TAG, "Moments player/broadcast activity visible");
        activityVisible = true;
    }

    public static void activityPaused() {
        Log.i(Util.TAG, "Moments player/broadcast activity NOT visible anymore");
        activityVisible = false;
    }

    private static boolean activityVisible;


    @Override
    public void onCreate(){
        super.onCreate();
        
        FacebookSdk.sdkInitialize(getApplicationContext());

        doCertMagic();

        NetworkStateListener.initialize();
     
    }

    private void initializeCertificate() {


        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    X509Certificate[] trustedAnchors = new X509Certificate[1];

                    //super.getAcceptedIssuers();

                    // Create a new array with room for an additional trusted certificate.
                    //                  X509Certificate[] myTrustedAnchors = new X509Certificate[trustedAnchors.length + 1];
                    //                  System.arraycopy(trustedAnchors, 0, myTrustedAnchors, 0, trustedAnchors.length);

                    // Load your certificate.

                    // Thanks to http://stackoverflow.com/questions/11857417/x509trustmanager-override-without-allowing-all-certs
                    // for this bit.

                    InputStream inStream = null;
                    X509Certificate cert = null;
                    try {
                        inStream = new FileInputStream("cert.der");
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        cert = (X509Certificate) cf.generateCertificate(inStream);
                        inStream.close();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (CertificateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Add your anchor cert as the last item in the array.
                    trustedAnchors[trustedAnchors.length] = cert;

                    return trustedAnchors;
                }

            }}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }



        try {

            // (could be from a resource or ByteArrayInputStream or ...)
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            InputStream caInput = new BufferedInputStream(getResources().openRawResource(R.raw.mobileman_ch));
            Certificate ca = cf.generateCertificate(caInput);
            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }

    }






    private static void doCertMagic() {
        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { return true; }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = createUnsecureSSLContext();
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {}
    }

    private static SSLContext createUnsecureSSLContext() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }};

        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { return true; }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        return sc;
    }

}
