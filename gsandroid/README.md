# GeoStruct — APK Android

App nativa che ospita GeoStruct in una WebView servita da
`https://appassets.androidplatform.net/` tramite `WebViewAssetLoader`.
È un'origine sicura reale: per questo i sensori (accelerometro,
magnetometro, orientamento) funzionano, mentre lo stesso file aperto
da `file://` o `content://` viene rifiutato con `NotAllowedError`.

Export CSV/JSON passano da un bridge nativo e finiscono in **Download**.

## Ottenere l'APK senza installare nulla (consigliato)

1. Crea una repository su GitHub (anche privata).
2. Carica **tutto il contenuto** di questa cartella nella root della repo
   (su github.com: *Add file → Upload files*, trascina le cartelle).
3. Vai su **Actions** → il workflow *Build APK* parte da solo.
   Se chiede conferma, premi *I understand my workflows, go ahead*.
4. A build finita (~4 minuti) apri il run → sezione **Artifacts** →
   scarica `GeoStruct-apk`.
5. Dentro trovi `app-debug.apk` e `app-release.apk`. Passa l'APK al
   telefono, aprilo, consenti l'installazione da origine sconosciuta.

Entrambi gli APK sono firmati con la chiave di debug: si installano
subito ma non sono pubblicabili sul Play Store. Per distribuirli, genera
un keystore tuo e sostituisci `signingConfig` in `app/build.gradle.kts`.

## Build da PC

Serve Android Studio (o solo il command-line SDK) e JDK 17:

    ./gradlew assembleDebug
    # oppure, senza wrapper:
    gradle assembleDebug

APK in `app/build/outputs/apk/debug/app-debug.apk`.

## Aggiornare l'app

Tutta l'applicazione è il singolo file
`app/src/main/assets/index.html`. Sostituiscilo, alza `versionCode`
in `app/build.gradle.kts` e ricompila.

## Struttura

    app/src/main/assets/index.html    l'applicazione (HTML+JS+CSS)
    app/src/main/java/.../MainActivity.java   host WebView + export nativo
    app/src/main/AndroidManifest.xml  dichiarazione sensori
    .github/workflows/build-apk.yml   compilazione automatica
