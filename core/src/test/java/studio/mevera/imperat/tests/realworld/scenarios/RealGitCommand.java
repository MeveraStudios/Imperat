package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models the real {@code git} CLI as faithfully as the framework's
 * one-class-per-root constraint allows. Each nested {@code @SubCommand}
 * mirrors a real git subcommand's positional/flag shape — see the
 * man-page reference next to each section header. Static {@code LAST_*}
 * fields capture the last-invoked arguments so the surrounding
 * {@code RealWorldCliScenariosTest} can assert against them without
 * stubbing a parser.
 *
 * <p>Replicates: {@code init}, {@code clone}, {@code add}, {@code status},
 * {@code commit}, {@code push}, {@code pull}, {@code fetch},
 * {@code checkout}, {@code branch}, {@code merge}, {@code log},
 * {@code diff}, {@code reset}, {@code tag}, {@code remote}. Anything
 * outside this list (rebase, cherry-pick, blame, worktree, submodule,
 * etc.) is omitted to keep the test fixture readable — the framework
 * surface they would exercise is identical to the included commands.</p>
 */
@RootCommand("git")
public final class RealGitCommand {

    // ---- commit (the original subset) -------------------------------
    public static volatile String LAST_MESSAGE;
    public static volatile Boolean LAST_AMEND;
    public static volatile Boolean LAST_NO_VERIFY;
    public static volatile Boolean LAST_ALL;

    // ---- init -------------------------------------------------------
    public static volatile String LAST_INIT_DIR;
    public static volatile Boolean LAST_INIT_BARE;
    public static volatile String LAST_INIT_BRANCH;

    // ---- clone ------------------------------------------------------
    public static volatile String LAST_CLONE_URL;
    public static volatile String LAST_CLONE_DIR;
    public static volatile Integer LAST_CLONE_DEPTH;
    public static volatile String LAST_CLONE_BRANCH;
    public static volatile Boolean LAST_CLONE_RECURSIVE;

    // ---- add --------------------------------------------------------
    public static volatile String LAST_ADD_PATHSPEC;
    public static volatile Boolean LAST_ADD_ALL;
    public static volatile Boolean LAST_ADD_FORCE;
    public static volatile Boolean LAST_ADD_PATCH;

    // ---- status -----------------------------------------------------
    public static volatile Boolean LAST_STATUS_SHORT;
    public static volatile Boolean LAST_STATUS_BRANCH;

    // ---- push -------------------------------------------------------
    public static volatile String LAST_PUSH_REMOTE;
    public static volatile String LAST_PUSH_BRANCH;
    public static volatile Boolean LAST_PUSH_FORCE;
    public static volatile Boolean LAST_PUSH_TAGS;
    public static volatile Boolean LAST_PUSH_SET_UPSTREAM;

    // ---- pull -------------------------------------------------------
    public static volatile String LAST_PULL_REMOTE;
    public static volatile String LAST_PULL_BRANCH;
    public static volatile Boolean LAST_PULL_REBASE;
    public static volatile Boolean LAST_PULL_FF_ONLY;

    // ---- fetch ------------------------------------------------------
    public static volatile String LAST_FETCH_REMOTE;
    public static volatile Boolean LAST_FETCH_ALL;
    public static volatile Boolean LAST_FETCH_PRUNE;
    public static volatile Boolean LAST_FETCH_TAGS;

    // ---- checkout ---------------------------------------------------
    public static volatile String LAST_CHECKOUT_TARGET;
    public static volatile Boolean LAST_CHECKOUT_NEW_BRANCH;
    public static volatile Boolean LAST_CHECKOUT_FORCE;

    // ---- branch -----------------------------------------------------
    public static volatile String LAST_BRANCH_NAME;
    public static volatile Boolean LAST_BRANCH_DELETE;
    public static volatile Boolean LAST_BRANCH_FORCE_DELETE;
    public static volatile Boolean LAST_BRANCH_LIST;
    public static volatile Boolean LAST_BRANCH_REMOTES;

    // ---- merge ------------------------------------------------------
    public static volatile String LAST_MERGE_BRANCH;
    public static volatile Boolean LAST_MERGE_NO_FF;
    public static volatile Boolean LAST_MERGE_SQUASH;
    public static volatile Boolean LAST_MERGE_ABORT;
    public static volatile String LAST_MERGE_MESSAGE;

    // ---- log --------------------------------------------------------
    public static volatile Boolean LAST_LOG_ONELINE;
    public static volatile Integer LAST_LOG_COUNT;
    public static volatile Boolean LAST_LOG_GRAPH;
    public static volatile String LAST_LOG_AUTHOR;

    // ---- diff -------------------------------------------------------
    public static volatile String LAST_DIFF_FROM;
    public static volatile String LAST_DIFF_TO;
    public static volatile Boolean LAST_DIFF_STAGED;
    public static volatile Boolean LAST_DIFF_NAME_ONLY;

    // ---- reset ------------------------------------------------------
    public static volatile String LAST_RESET_TARGET;
    public static volatile Boolean LAST_RESET_HARD;
    public static volatile Boolean LAST_RESET_SOFT;
    public static volatile Boolean LAST_RESET_MIXED;

    // ---- tag --------------------------------------------------------
    public static volatile String LAST_TAG_NAME;
    public static volatile String LAST_TAG_COMMIT;
    public static volatile String LAST_TAG_MESSAGE;
    public static volatile Boolean LAST_TAG_DELETE;
    public static volatile Boolean LAST_TAG_LIST;

    // ---- remote -----------------------------------------------------
    public static volatile String LAST_REMOTE_ACTION;
    public static volatile String LAST_REMOTE_NAME;
    public static volatile String LAST_REMOTE_URL;

    // ============================================================
    // git init [<directory>] [--bare] [--initial-branch=<name>]
    // ============================================================
    @SubCommand("init")
    public static final class Init {

        @Execute
        public void run(
                TestCommandSource sender,
                @Optional @Default(".") String directory,
                @Switch("bare") Boolean bare,
                @Flag({"initial-branch", "b"}) @Default("main") String initialBranch
        ) {
            LAST_INIT_DIR = directory;
            LAST_INIT_BARE = bare;
            LAST_INIT_BRANCH = initialBranch;
        }
    }

    // ============================================================
    // git clone <url> [<directory>] [--depth <n>] [--branch <name>] [--recursive]
    // ============================================================
    @SubCommand("clone")
    public static final class Clone {

        @Execute
        public void run(
                TestCommandSource sender,
                String url,
                @Optional String directory,
                @Flag({"depth", "d"}) @Default("0") Integer depth,
                @Flag({"branch", "b"}) String branch,
                @Switch("recursive") Boolean recursive
        ) {
            LAST_CLONE_URL = url;
            LAST_CLONE_DIR = directory;
            LAST_CLONE_DEPTH = depth;
            LAST_CLONE_BRANCH = branch;
            LAST_CLONE_RECURSIVE = recursive;
        }
    }

    // ============================================================
    // git add <pathspec> [--all] [--force] [--patch]
    // ============================================================
    @SubCommand("add")
    public static final class Add {

        @Execute
        public void run(
                TestCommandSource sender,
                String pathspec,
                @Switch({"all", "A"}) Boolean all,
                @Switch({"force", "f"}) Boolean force,
                @Switch({"patch", "p"}) Boolean patch
        ) {
            LAST_ADD_PATHSPEC = pathspec;
            LAST_ADD_ALL = all;
            LAST_ADD_FORCE = force;
            LAST_ADD_PATCH = patch;
        }
    }

    // ============================================================
    // git status [--short] [--branch]
    // ============================================================
    @SubCommand("status")
    public static final class Status {

        @Execute
        public void run(
                TestCommandSource sender,
                @Switch({"short", "s"}) Boolean shortFmt,
                @Switch({"branch", "b"}) Boolean branch
        ) {
            LAST_STATUS_SHORT = shortFmt;
            LAST_STATUS_BRANCH = branch;
        }
    }

    // ============================================================
    // git commit -m "<msg>" [--amend] [--no-verify] [-a]
    // ============================================================
    @SubCommand("commit")
    public static final class Commit {

        @Execute
        public void run(
                TestCommandSource sender,
                @Flag({"message", "m"}) String message,
                @Switch("amend") Boolean amend,
                @Switch("no-verify") Boolean noVerify,
                @Switch({"all", "a"}) Boolean all
        ) {
            LAST_MESSAGE = message;
            LAST_AMEND = amend;
            LAST_NO_VERIFY = noVerify;
            LAST_ALL = all;
        }
    }

    // ============================================================
    // git push [<remote>] [<branch>] [--force] [--tags] [-u/--set-upstream]
    // ============================================================
    @SubCommand("push")
    public static final class Push {

        @Execute
        public void run(
                TestCommandSource sender,
                @Optional @Default("origin") String remote,
                @Optional String branch,
                @Switch({"force", "f"}) Boolean force,
                @Switch("tags") Boolean tags,
                @Switch({"set-upstream", "u"}) Boolean setUpstream
        ) {
            LAST_PUSH_REMOTE = remote;
            LAST_PUSH_BRANCH = branch;
            LAST_PUSH_FORCE = force;
            LAST_PUSH_TAGS = tags;
            LAST_PUSH_SET_UPSTREAM = setUpstream;
        }
    }

    // ============================================================
    // git pull [<remote>] [<branch>] [--rebase] [--ff-only]
    // ============================================================
    @SubCommand("pull")
    public static final class Pull {

        @Execute
        public void run(
                TestCommandSource sender,
                @Optional @Default("origin") String remote,
                @Optional String branch,
                @Switch("rebase") Boolean rebase,
                @Switch("ff-only") Boolean ffOnly
        ) {
            LAST_PULL_REMOTE = remote;
            LAST_PULL_BRANCH = branch;
            LAST_PULL_REBASE = rebase;
            LAST_PULL_FF_ONLY = ffOnly;
        }
    }

    // ============================================================
    // git fetch [<remote>] [--all] [--prune] [--tags]
    // ============================================================
    @SubCommand("fetch")
    public static final class Fetch {

        @Execute
        public void run(
                TestCommandSource sender,
                @Optional @Default("origin") String remote,
                @Switch("all") Boolean all,
                @Switch({"prune", "p"}) Boolean prune,
                @Switch("tags") Boolean tags
        ) {
            LAST_FETCH_REMOTE = remote;
            LAST_FETCH_ALL = all;
            LAST_FETCH_PRUNE = prune;
            LAST_FETCH_TAGS = tags;
        }
    }

    // ============================================================
    // git checkout <branch> [-b] [--force]
    // (-b creates a new branch from the current HEAD)
    // ============================================================
    @SubCommand("checkout")
    public static final class Checkout {

        @Execute
        public void run(
                TestCommandSource sender,
                String target,
                @Switch({"new-branch", "b"}) Boolean newBranch,
                @Switch({"force", "f"}) Boolean force
        ) {
            LAST_CHECKOUT_TARGET = target;
            LAST_CHECKOUT_NEW_BRANCH = newBranch;
            LAST_CHECKOUT_FORCE = force;
        }
    }

    // ============================================================
    // git branch [<name>] [-d <name>] [-D <name>] [--list] [--remotes]
    // ============================================================
    @SubCommand("branch")
    public static final class Branch {

        @Execute
        public void run(
                TestCommandSource sender,
                @Optional String name,
                @Switch({"delete", "d"}) Boolean delete,
                @Switch({"force-delete", "D"}) Boolean forceDelete,
                @Switch({"list", "l"}) Boolean list,
                @Switch({"remotes", "r"}) Boolean remotes
        ) {
            LAST_BRANCH_NAME = name;
            LAST_BRANCH_DELETE = delete;
            LAST_BRANCH_FORCE_DELETE = forceDelete;
            LAST_BRANCH_LIST = list;
            LAST_BRANCH_REMOTES = remotes;
        }
    }

    // ============================================================
    // git merge <branch> [--no-ff] [--squash] [--abort] [-m <msg>]
    // ============================================================
    @SubCommand("merge")
    public static final class Merge {

        @Execute
        public void run(
                TestCommandSource sender,
                @Optional String branch,
                @Switch("no-ff") Boolean noFf,
                @Switch("squash") Boolean squash,
                @Switch("abort") Boolean abort,
                @Flag({"message", "m"}) String message
        ) {
            LAST_MERGE_BRANCH = branch;
            LAST_MERGE_NO_FF = noFf;
            LAST_MERGE_SQUASH = squash;
            LAST_MERGE_ABORT = abort;
            LAST_MERGE_MESSAGE = message;
        }
    }

    // ============================================================
    // git log [--oneline] [-n <count>] [--graph] [--author=<name>]
    // ============================================================
    @SubCommand("log")
    public static final class Log {

        @Execute
        public void run(
                TestCommandSource sender,
                @Switch("oneline") Boolean oneline,
                @Flag({"count", "n"}) @Default("0") Integer count,
                @Switch("graph") Boolean graph,
                @Flag("author") String author
        ) {
            LAST_LOG_ONELINE = oneline;
            LAST_LOG_COUNT = count;
            LAST_LOG_GRAPH = graph;
            LAST_LOG_AUTHOR = author;
        }
    }

    // ============================================================
    // git diff [<from>] [<to>] [--staged] [--name-only]
    // ============================================================
    @SubCommand("diff")
    public static final class Diff {

        @Execute
        public void run(
                TestCommandSource sender,
                @Optional String from,
                @Optional String to,
                @Switch("staged") Boolean staged,
                @Switch("name-only") Boolean nameOnly
        ) {
            LAST_DIFF_FROM = from;
            LAST_DIFF_TO = to;
            LAST_DIFF_STAGED = staged;
            LAST_DIFF_NAME_ONLY = nameOnly;
        }
    }

    // ============================================================
    // git reset [<commit>] [--hard] [--soft] [--mixed]
    // ============================================================
    @SubCommand("reset")
    public static final class Reset {

        @Execute
        public void run(
                TestCommandSource sender,
                @Optional @Default("HEAD") String target,
                @Switch("hard") Boolean hard,
                @Switch("soft") Boolean soft,
                @Switch("mixed") Boolean mixed
        ) {
            LAST_RESET_TARGET = target;
            LAST_RESET_HARD = hard;
            LAST_RESET_SOFT = soft;
            LAST_RESET_MIXED = mixed;
        }
    }

    // ============================================================
    // git tag <name> [<commit>] [-m <msg>] [-d <name>] [--list]
    // ============================================================
    @SubCommand("tag")
    public static final class Tag {

        @Execute
        public void run(
                TestCommandSource sender,
                @Optional String name,
                @Optional String commit,
                @Flag({"message", "m"}) String message,
                @Switch({"delete", "d"}) Boolean delete,
                @Switch({"list", "l"}) Boolean list
        ) {
            LAST_TAG_NAME = name;
            LAST_TAG_COMMIT = commit;
            LAST_TAG_MESSAGE = message;
            LAST_TAG_DELETE = delete;
            LAST_TAG_LIST = list;
        }
    }

    // ============================================================
    // git remote [<action>] [<name>] [<url-or-target>...]
    // - git remote                       (lists remotes)
    // - git remote add <name> <url>
    // - git remote remove <name>
    // - git remote rename <old> <new>
    // ============================================================
    @SubCommand("remote")
    public static final class Remote {

        @Execute
        public void run(
                TestCommandSource sender,
                @Optional @Default("list") String action,
                @Optional String name,
                @Optional @Greedy String urlOrTarget
        ) {
            LAST_REMOTE_ACTION = action;
            LAST_REMOTE_NAME = name;
            LAST_REMOTE_URL = urlOrTarget;
        }
    }
}
