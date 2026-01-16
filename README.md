# roughtime-client (Clojure)

A tiny Clojure CLI client for [Roughtime](https://datatracker.ietf.org/doc/draft-ietf-ntp-roughtime/) servers.
It crafts a request, verifies the response, and prints the verified time and skew.

While this may be used as a CLI client, its primary purpose is to demonstrate how to make Roughtime requests in client applications.

## Quick start

Requires the Clojure CLI (`clj` / `clojure`).

```bash
# Query a named server from the built-in ecosystem:
clj -M:run :server-name SturdyStatistics

# Verbose (pretty-print request/response frames):
clj -M:run :server-name int08h :print-request :print-response

# Chain a request through known servers
clj -M:run :chain
```

Example output:

```
{:skew 0,
 :online-key-expires-in "∞",
 :maxt 18446744073709551615,
 :midp 1761408075,
 :local-time 1761408075}
```

You can also pass explicit server parameters (no ecosystem lookup):

```bash
clj -M:run :address "roughtime.int08h.com:2002" :public-key "AW5uAoTSTDfG5NfY1bTh08GUnOqlRb+HVhbJ3ODJvsE=" :version-no "0x8000000c" :protocol "udp"
```

## CLI options

These options are parsed/validated via `babashka.cli`. Defaults:

```clojure
{:protocol "udp"
 :msg-size 1024
 :print-request false
 :print-response false}
```

| Option            | Alias | Type    | Default | Description                                                       |
|-------------------|-------|---------|---------|-------------------------------------------------------------------|
| `:server-name`    | `:s`  | string  | —       | Name from the built-in ecosystem (see below).                     |
| `:address`        | `:a`  | string  | —       | `host:port` (IPv4/IPv6 or DNS), e.g. `roughtime.int08h.com:2002`. |
| `:protocol`       | `:p`  | string  | `udp`   | Transport protocol.                                               |
| `:public-key`     | `:k`  | base64  | —       | Server’s Ed25519 public key (base64).                             |
| `:version-no`     | `:V`  | string  | —       | Roughtime version (integer or hex), e.g. `"0x8000000c"`.          |
| `:msg-size`       | `:n`  | long    | `1024`  | Pad messages to this size (bytes).                                |
| `:print-request`  | —     | boolean | `false` | Pretty print the serialized request map/frame.                    |
| `:print-response` | —     | boolean | `false` | Pretty print the server response (CERT, SREP, PATH, etc.).        |

### Selection logic

- If `:server-name` is provided, the client loads connection parameters from the *ecosystem* (below) and applies CLI overrides (e.g., `:protocol`, `:msg-size`).
- Otherwise, it expects at least `:address`, `:public-key`, and `:version-no`.

## Built-in ecosystem

A small curated set of known servers:

```clojure
{
 "Cloudflare"
 {:name "Cloudflare-Roughtime-2"
  :version "IETF-Roughtime"
  :version-no "0b000080"
  :msg-size 1012
  :public-key "0GD7c3yP8xEc4Zl2zeuN2SlLvDVVocjsPSL8/Rl/7zg="
  :addresses [{:protocol "udp" :address "roughtime.cloudflare.com:2003"}]}

 "int08h"
 {:name "int08h-Roughtime"
  :version "IETF-Roughtime"
  :version-no "0c000080"
  :msg-size 1024
  :public-key "AW5uAoTSTDfG5NfY1bTh08GUnOqlRb+HVhbJ3ODJvsE="
  :addresses [{:protocol "udp" :address "roughtime.int08h.com:2002"}]}

 "roughtime.se"
 {:name "roughtime.se"
  :version "IETF-Roughtime"
  :version-no "0c000080"
  :msg-size 1024
  :public-key "S3AzfZJ5CjSdkJ21ZJGbxqdYP/SoE8fXKY0+aicsehI="
  :addresses [{:protocol "udp" :address "roughtime.se:2002"}]}

 "TXRyan"
 {
  :name "time.txryan.com"
  :version "Google-Roughtime"
  :version-no 0x00
  :supported-versions nil
  :msg-size 1024
  :public-key-type "ed25519"
  :public-key "iBVjxg/1j7y1+kQUTBYdTabxCppesU/07D4PMDJk2WA="
  :addresses [ {:protocol "udp" :address "time.txryan.com:2002"}]}

 "SturdyStatistics"
 {
  :name "Sturdy-Statistics"
  :version "IETF-Roughtime"
  :version-no 0x8000000c
  :supported-versions nil
  :msg-size 1024
  :public-key-type "ed25519"
  :public-key "NqIjwLopQn6yQChtE21Mb97dAbAPe5UOuTa0tOakgD8="
  :addresses [{:protocol "udp" :address "roughtime.sturdystatistics.com:2002"}]}
}
```

## Request Chaining

You can also chain requests.
The command
```bash
clj -M:run :chain
```
Will chain a request through all servers in the ecosystem and report a bad actor if one is found.

Example output:
```
Running chain script; ignoring all other options.

All servers are consistent.

|                  :name |                                  :public-key | :lower-limit | :upper-limit | :expires-in |
|------------------------+----------------------------------------------+--------------+--------------+-------------|
|       int08h-Roughtime | AW5uAoTSTDfG5NfY1bTh08GUnOqlRb+HVhbJ3ODJvsE= |           -5 |            5 |           ∞ |
| Cloudflare-Roughtime-2 | 0GD7c3yP8xEc4Zl2zeuN2SlLvDVVocjsPSL8/Rl/7zg= |           -1 |            1 |     13h 12m |
|      Sturdy-Statistics | NqIjwLopQn6yQChtE21Mb97dAbAPe5UOuTa0tOakgD8= |          -10 |           10 |      1d 11h |
|           roughtime.se | S3AzfZJ5CjSdkJ21ZJGbxqdYP/SoE8fXKY0+aicsehI= |           -1 |            1 |      14d 2h |
|        time.txryan.com | iBVjxg/1j7y1+kQUTBYdTabxCppesU/07D4PMDJk2WA= |           -1 |            1 |      2y 5mo |
```

## Examples

Query a named server:

```bash
clj -M:run :server-name SturdyStatistics
```

```
{:skew 0,
 :online-key-expires-in "∞",
 :maxt 18446744073709551615,
 :midp 1768457585,
 :local-time 1768457585}
```


Verbose request/response:

```bash
clj -M:run :server-name int08h :print-request :print-response
```

```
==== REQUEST ====
{"VER" "0x8000000c",
 "SRV"
 ["4a4344b8 ed36a263 cae105a2 92fe2661"
  "d964691d be07548f 43b5b523 2671f3ab"],
 "NONC"
 ["5e74bbfb 6902f802 9c464d0c e0a94bf5"
  "a4f2f803 fff56505 13d319c8 824dcfff"],
 "TYPE" 0,
 "ZZZZ" "0{912}"}

==== RESPONSE ====
{"SIG"
 ["2359bfd7 f3c4fa29 6847c016 40938256"
  "344c5c91 cdede673 372fc79f 4332a482"
  "7f2ffc4d 478c2295 413d57d5 505170ff"
  "aa11f3e2 c609453d be14192c 2edb060e"],
 "NONC"
 ["5e74bbfb 6902f802 9c464d0c e0a94bf5"
  "a4f2f803 fff56505 13d319c8 824dcfff"],
 "TYPE" 1,
 "PATH" [],
 "SREP"
 {"VER" "0x8000000c",
  "RADI" 10,
  "MIDP" 1768457531,
  "VERS"
  ("0"
   "0x80000001"
   "0x80000002"
   "0x80000003"
   "0x80000004"
   "0x80000006"
   "0x80000008"
   "0x80000009"
   "0x8000000a"
   "0x8000000b"
   "0x8000000c"),
  "ROOT"
  ["f5681bf4 c46ee065 3f21988c 23ed8708"
   "9461c0c8 b2267f4e f2720269 0105d74f"]},
 "CERT"
 {"SIG"
  ["c8735ec3 502f2b25 c4855de8 91e8ce24"
   "cf6b09c9 c954a958 ef7915ad f47c1097"
   "7f74063c e4b5c528 e3075c6f 0ca93ef3"
   "074892f6 4cb2608a 10a6ea36 0132c406"],
  "DELE"
  {"PUBK"
   ["c51da875 891887e1 9557784c d8e3b163"
    "990dd688 851633f6 dcced7de fcd9fcc2"],
   "MINT" 1768450687,
   "MAXT" 1768623487}},
 "INDX" 0}

==== RESULT ====
{:skew 0,
 :online-key-expires-in "1d 22h",
 :maxt 1768623487,
 :midp 1768457531,
 :local-time 1768457531}
```

Direct parameters (no ecosystem):

```bash
# Loopback example (adjust to your server):
clj -M:run :address "127.0.0.1:2002" :protocol "udp" :public-key "dWPfGcRTe9nbILRO3cyJsWFHUnSF2AEKQ6g2530UNHA=" :version-no "0x8000000c"

# Public server (int08h):
clj -M:run :version-no "0x8000000c" :public-key "AW5uAoTSTDfG5NfY1bTh08GUnOqlRb+HVhbJ3ODJvsE=" :address "roughtime.int08h.com:2002"
```

## License

Apache License 2.0

Copyright © Sturdy Statistics

<!-- Local Variables: -->
<!-- fill-column: 100000 -->
<!-- End: -->
