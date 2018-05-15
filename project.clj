(defproject cloiure "x.y.z"
    :dependencies [[org.clojure/clojure "1.9.0"]
                   [org.clojure/spec.alpha "0.1.143"]
                   [org.clojure/core.specs.alpha "0.1.24"]
                   [org.graalvm/graal-sdk "1.0.0-rc1"]
                   [com.oracle.truffle/truffle-api "1.0.0-rc1"]
                   [org.openjdk.jol/jol-core "0.9"]
                   [org.openjdk.jol/jol-cli "0.9"]]
    :plugins [[lein-try "0.4.3"]]
;   :global-vars {*warn-on-reflection* true}
    :jvm-opts ["-Xmx12g"]
    :javac-options ["-g"
                    "--add-modules=jdk.internal.vm.ci"
                    "--add-exports=jdk.internal.vm.ci/jdk.vm.ci.amd64=ALL-UNNAMED"
                    "--add-exports=jdk.internal.vm.ci/jdk.vm.ci.code.site=ALL-UNNAMED"
                    "--add-exports=jdk.internal.vm.ci/jdk.vm.ci.code.stack=ALL-UNNAMED"
                    "--add-exports=jdk.internal.vm.ci/jdk.vm.ci.code=ALL-UNNAMED"
                    "--add-exports=jdk.internal.vm.ci/jdk.vm.ci.common=ALL-UNNAMED"
                    "--add-exports=jdk.internal.vm.ci/jdk.vm.ci.hotspot=ALL-UNNAMED"
                    "--add-exports=jdk.internal.vm.ci/jdk.vm.ci.meta=ALL-UNNAMED"
                    "--add-exports=jdk.internal.vm.ci/jdk.vm.ci.runtime=ALL-UNNAMED"
                    "--add-exports=jdk.internal.vm.ci/jdk.vm.ci.services=ALL-UNNAMED"]
    :source-paths ["src"] :java-source-paths ["src"] :resource-paths ["resources"] :test-paths ["src"]
    :main cloiure.core
    :aliases {"cloiure" ["run" "-m" "cloiure.core"]})
