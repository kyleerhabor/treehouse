# Treehouse

[Website](https://kyleerhabor.com/)

My own little website.

## Install

To install, begin by cloning the repository.

```sh
git clone https://github.com/KyleErhabor/treehouse
```

You'll need it to run aliases (scripts) and set up configuration, but not necessarily to run the project.

### Config

[cprop](https://github.com/tolitius/cprop) is used for configuration, meaning it accepts a number of sources to
merge into a config:
1. A `resources/config.edn` file
2. A path to a file in the `conf` Java system property (`-Dconf=...`)
3. Java system properties
4. Environment variables

Properties found in later sources will merge/override properties in previous sources. The resulting config should
conform to the following structure:

```clojure
{;; The port the server should run on.
 :kyleerhabor.treehouse.server/port ...
 ;; The email to display on the home page.
 :kyleerhabor.treehouse.server.config/email ...
 ;; The path to the database. Can be a directory or Datalevin connection URI.
 :kyleerhabor.treehouse.server.database/dir ...
 ;; The Discord application client ID to use when exchanging tokens.
 :kyleerhabor.treehouse.server.remote.discord/client-id ...
 ;; The Discord application client secret to use when exchanging tokens.
 :kyleerhabor.treehouse.server.remote.discord/client-secret ...
 ;; The redirect URI set up for Discord authentication.
 :kyleerhabor.treehouse.server.remote.discord/redirect ...
 ;; The GitHub personal access token to use for requests.
 :kyleerhabor.treehouse.server.remote.github/token ...}
```

### Discord

A [Discord application](https://discord.com/developers/applications) must be used to communincate with Discord.
1. On the OAuth2 page, insert the redirect URI to be used by the project
2. On the URL Generator page, select the identify scope and redirect URI mentioned previously, and use the generated URL
to authorize the application
3. In the URL that was redirected to, copy the code query parameter value and run `clojure -X:server:discord :code '"..."'`
where `...` is the code.

The last step will exchange the token for an access and refresh token the project will use to communicate with Discord.
If you already have one, run `clojure -X:server:discord :token '"..."' :refresh '"..."'` instead where token associates
the access token and refresh associates the refresh token.

### Build

To compile the project, run `clojure -T:build uberjar`. This will compile ClojureScript to `resources/public/assets/main/js/compiled/main.js` and produce an uberjar (a Java jar with dependencies) in `target/treehouse-...-standalone.jar`, where `...` is
the current version. As the uberjar is just a Java jar, it can be compiled locally and distributed elsewhere (e.g. to a
host).

## Running

To run the project, run `java -jar target/treehouse-...-standalone.jar`, where `...` is the current version. The homa
page can then be found at `http://localhost:.../`, where `...` is the port used in the config.s

## License

Licensed under the [Cooperative Software License](./LICENSE).
