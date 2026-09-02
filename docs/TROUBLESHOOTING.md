# Troubleshooting

Real issues hit while setting up this project on this machine, and their fixes.

## `winget` / Maven Central downloads fail with SSL/certificate errors

**Symptom:** `winget install` fails with "server certificate did not match any of the
expected values"; `mvn compile` fails with `PKIX path building failed ... unable to find
valid certification path`.

**Cause:** Avast Antivirus's "Web/Mail Shield" performs TLS interception (MITM) for
HTTPS scanning on this machine, using a locally-generated root certificate
(`CN=Avast Web/Mail Shield Root`). It's trusted by Windows (installed in the Windows
Root store) but **not** by the JDK's own `cacerts` truststore, which Maven/Java use
independently of Windows.

**Fix used:**
```powershell
# Copy the JDK's cacerts somewhere writable (Program Files requires admin to edit directly)
Copy-Item "$env:JAVA_HOME\lib\security\cacerts" "$env:USERPROFILE\tools\my-cacerts.jks" -Force

# Import Avast's exported root cert (it drops a PEM copy here)
& "$env:JAVA_HOME\bin\keytool.exe" -importcert -alias avast-root `
  -keystore "$env:USERPROFILE\tools\my-cacerts.jks" -storepass changeit `
  -file "C:\ProgramData\Avast Software\Avast\wscert.pem" -noprompt

# Point Maven at it
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStore=$env:USERPROFILE\tools\my-cacerts.jks -Djavax.net.ssl.trustStorePassword=changeit"
```
This needs to be set in every new shell session (`MAVEN_OPTS` isn't persisted).
A more permanent fix would be adding the Avast cert directly into the real
`%JAVA_HOME%\lib\security\cacerts` — requires admin rights, not attempted here.

## Maven not on PATH

Maven wasn't installed system-wide; it was downloaded manually from
`https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip`
(NOT `dlcdn.apache.org`, which only hosts the current release and 404s for older
versions) and extracted to `%USERPROFILE%\tools\apache-maven-3.9.9`. Invoke it via full
path (`$env:USERPROFILE\tools\apache-maven-3.9.9\bin\mvn.cmd`) or add that `bin/` to
`PATH` for the session.

## `Advice.to()` / classloader issues when instrumenting JDK bootstrap classes

See `docs/DESIGN_DECISIONS.md` §4 for the three real classloader bugs hit and fixed
while wiring up the `ProcessBuilder.start()` sensor. If you add a new sensor on another
bootstrap-loaded JDK class (deserialization, JNDI, script eval all qualify), expect to
re-encounter variants of these — the fixes documented there (resolve Advice before
bootstrap injection; never use a shared/consumed class as the injection marker; always
filter the injected jar to `provguard/` only) apply generally, not just to this one sensor.

## JDK version

This machine only had JDK 26 (very new, non-LTS) installed — see
`docs/DESIGN_DECISIONS.md` §2. `-Dnet.bytebuddy.experimental=true` was required to get
ByteBuddy to proceed on it. Re-verify on JDK 21 LTS before trusting this beyond a demo.
