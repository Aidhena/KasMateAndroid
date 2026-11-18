# KasMate Android setup

The Android app uses the same MariaDB data as the ClassFinance desktop app through the XAMPP PHP API.

## Connect phones on the same Wi-Fi

1. On the computer that runs XAMPP, start **Apache** and **MySQL**.
2. Keep the KasMate API deployed at `D:\xampp\htdocs\KasMateAPI`.
3. Connect the phone and computer to the same trusted Wi-Fi network.
4. The Android app connects to the computer's current LAN IP, `10.199.241.182`, using `http://10.199.241.182/KasMateAPI/`.
5. Sign in with the student's name and password. Enter NIS when more than one student has the same name.

The server IP can change when the computer reconnects to Wi-Fi. If it changes, update `DEFAULT_ADDRESS` in `app/src/main/java/com/smktel/kasmate/data/KasMateServerConfig.kt`, then rebuild and install the app. Saved addresses from older builds migrate to the current default on launch. Apache must accept inbound HTTP traffic on port 80; allow Apache on the Private network in Windows Firewall when needed.

## Student accounts

Student accounts use existing `students` rows. In the ClassFinance desktop app, open **Data Siswa**, open the student's action menu, and choose **Atur password aplikasi · 12345678**. The server stores a password hash and revokes that student's earlier Android sessions. Students sign in with their registered name, optional NIS, and the password set by staff.

Migration `009_android_student_auth.sql` adds the student password hash and student session table. It is already applied to the current KasMate database.

## Notifications while the app is closed

The Android app uses Firebase Cloud Messaging (FCM) for desktop-created tagihan and reminder alerts.
Add a Firebase Android app with package `com.smktel.kasmate` and place its `google-services.json`
in the Android `app` directory. The build keeps working without this file, but closed-app push is
disabled until Firebase is configured.

On the API server, apply migrations `ClassFinance/Database/010_android_push_tokens.sql` and
`ClassFinance/Database/011_android_fcm_installation_ids.sql`. Configure Apache with
`KASMATE_FIREBASE_PROJECT_ID` and `KASMATE_FIREBASE_SERVICE_ACCOUNT`; keep the service-account JSON
outside `htdocs`, enable the FCM HTTP v1 API, and grant the service account FCM send permission.
Restart Apache after setting the variables. Each student device registers its Firebase installation
ID when that student signs in. The existing polling fallback runs when the app is opened. Android itself blocks app delivery
after the user explicitly force-stops the app until it is opened again.

## Student payment requests

Apply `ClassFinance/Database/012_android_payment_requests.sql` and
`ClassFinance/Database/013_android_payment_proof_filename.sql` to enable the payment request queue
and retain the selected proof filename.
Students choose QRIS, transfer/e-wallet, or cash and attach a JPG, PNG, WebP, or PDF proof file (up to
5 MB). Proofs are stored outside `htdocs`, with the uploaded filename retained for review.
Admin/Bendahara users can review requests from the desktop notification bell or the **Permintaan
Pembayaran** page. Approval records the income
transaction and updates the bill and class balance together; rejection leaves the bill unpaid and
notifies the student.

## Network security

The current XAMPP API uses HTTP for local-network access. Use it only on a trusted private network. Use HTTPS before exposing the API outside that network.
