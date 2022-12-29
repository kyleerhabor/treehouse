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
{;; The URL to the project homepage (i.e. info on the project, could be a GitHub repo)
 :kyleerhabor.treehouse.config/source ...
 ;; The port the server should run on.
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

#### Logging

[Timbre](https://github.com/ptaoussanis/timbre) is used for logging. While the project handles most configuration, the
`TAOENSSO_TIMBRE_MIN_LEVEL_EDN` environment variable or `taoensso.timbre.min-level.edn` Java system property can be set
when building to elide logs under a level, which is useful in production for performance.

### Discord

A [Discord application](https://discord.com/developers/applications) must be used to communincate with Discord.
1. On the OAuth2 page, insert the redirect URI to be used by the project
2. On the URL Generator page, select the identify scope and redirect URI mentioned previously, and use the generated URL
to authorize the application
3. In the URL that was redirected to, copy the code query parameter value and run `clojure -X:server:discord :code '"..."'`,
where `...` is the code

The last step will exchange the token for an access and refresh token the project will use to communicate with Discord.
If you already have one, run `clojure -X:server:discord :token '"..."' :refresh '"..."'` instead, where `:token`
associates the access token and `:refresh` associates the refresh token.

### GitHub

GitHub requires a personal access token to communicate with the API. [Create a token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token#creating-a-personal-access-token-classic)
with the `read:user` scope and save it for configuration. At the time of writing this, fine-grained personal access
tokens are not supported for the server's operations, so ignore the suggestion to create one.

### Build

The `build` alias with the `uberjar` option can be used to compile the project. The alias accepts an `:include` option
for selecting files to be packaged into the produced uberjar (a JAR with dependencies) from the `src` and `resources`
folders. By default, `:include` lists the following:
- `src/main/kyleerhabor`
- `resources/project.edn`
- `resources/content`
- `resources/articles`
- `resources/public/robots.txt`
- `resources/public/assets/main/js/compiled/main.js`
- `resources/public/assets/main/js/compiled/main.js.map`
- `resources/public/assets/main/css/compiled/main.css`

To provide a list, pass a collection with `:include`.

```sh
clojure -T:build uberjar :include '["..." "..." ...]'
```

The build will compile ClojureScript to the `compiled` folders under `resources/public/assets` and produce an uberjar in
`target/treehouse-...-standalone.jar`, where `...` is the current version. As the uberjar is just a JAR, it can be
compiled locally and distributed elsewhere (e.g. to a host/server).

## Running

The `java` command with the `-jar` option can be used to run the project.

```sh
java --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED -jar target/treehouse-...-standalone.jar
```

The `--add-opens` options are required for the database. When using environment variables on the command line, the `env`
command can be used to handle keys with dots. A more complete version of the command to run the project can be seen below.

```sh
env KYLEERHABOR.TREEHOUSE.SERVER.REMOTE.DISCORD___CLIENT_SECRET=... KYLEERHABOR.TREEHOUSE.SERVER.REMOTE.GITHUB___TOKEN=... java -Dconf=... --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED -jar target/treehouse-...-standalone.jar
```

After the server starts, navigate to `http://localhost:.../` to see the home page, with `...` representing the port used in the config.

## License

Licensed under the [Cooperative Software License](./LICENSE).
