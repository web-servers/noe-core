package noe.common.utils;

import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class for basic Git operations (clone, checkout).
 */
public class GitUtils {

    private static final Logger log = LoggerFactory.getLogger(GitUtils.class);

    private GitUtils() {
    }

    /**
     * Clone repository locally and checkout ref.
     *
     * @param repoUrl         URL to GIT repository
     * @param ref             ref to checkout (tag or branch). If null is set then branch defined in HEAD will be checked out.
     * @param targetDirectory directory where repository will be cloned
     * @return true upon success
     */
    public static boolean cloneCheckout(final String repoUrl, final String ref, final File targetDirectory) {
        return cloneCheckout(repoUrl, ref, targetDirectory, new ArrayList<String>());
    }

    /**
     * Clone repository locally and checkout ref.
     *
     * @param repoUrl         URL to GIT repository
     * @param ref             ref to checkout (tag or branch). If null is set then branch defined in HEAD will be checked out.
     * @param targetDirectory directory where repository will be cloned
     * @param toWait          time to wait between trying to clone of git repository
     * @param maxAttempts     maximum attempts to try to clone git
     * @return true upon success
     */
    public static boolean cloneCheckout(final String repoUrl, final String ref, final File targetDirectory, final long toWait, final int maxAttempts) {
        return cloneCheckout(repoUrl, ref, targetDirectory, new ArrayList<String>(), toWait, maxAttempts);
    }

    /**
     * Clone repository locally and checkout ref.
     *
     * @param repoUrl         URL to GIT repository
     * @param ref             ref to checkout (tag or branch). If null is set then branch defined in HEAD will be checked out.
     * @param targetDirectory directory where repository will be cloned
     * @param filesToValidate files which should exist after checkout
     * @return true upon success
     */
    public static boolean cloneCheckout(final String repoUrl, final String ref, final File targetDirectory,
                                        final List<String> filesToValidate) {
        checkArgument(!Strings.isNullOrEmpty(repoUrl), "Cannot clone any git repository from null url");
        checkNotNull(targetDirectory, "Cannot clone into null directory");

        return tryClone(repoUrl, ref, targetDirectory, filesToValidate, 30000, 4);
    }

    /**
     * Clone repository locally and checkout ref.
     *
     * @param repoUrl         URL to GIT repository
     * @param ref             ref to checkout (tag or branch). If null is set then branch defined in HEAD will be checked out.
     * @param targetDirectory directory where repository will be cloned
     * @param filesToValidate files which should exist after checkout
     * @return true upon success
     */
    public static boolean cloneCheckout(final String repoUrl, final String ref, final File targetDirectory,
                                        final List<String> filesToValidate, final long toWait, final int maxAttempts) {
        checkArgument(!Strings.isNullOrEmpty(repoUrl), "Cannot clone any git repository from null url");
        checkNotNull(targetDirectory, "Cannot clone into null directory");

        return tryClone(repoUrl, ref, targetDirectory, filesToValidate, toWait, maxAttempts);
    }

    /**
     * Try to clone repository locally maxAttempts times.
     *
     * @param repoUrl         URL to GIT repository
     * @param ref             ref to checkout (tag or branch). If null is set then branch defined in HEAD will be checked out.
     * @param targetDirectory directory where repository will be cloned
     * @param filesToValidate files which should exist after checkout
     * @param toWait          time to wait between trying to clone of git repository
     * @param maxAttempts     maximum attempts to try to clone git
     * @return true upon success
     */
    private static boolean tryClone(final String repoUrl, final String ref, final File targetDirectory,
                                    final List<String> filesToValidate, final long toWait, final int maxAttempts) {
        long waiting = toWait;
        int attempts = 1;
        while (attempts <= maxAttempts) {
            try {
                log.info("Attempt " + attempts + " of " + maxAttempts);
                FileUtils.deleteDirectory(targetDirectory);

                Git git = clone(repoUrl, ref, targetDirectory);
                checkout(ref, git);

                return expectedFilesArePresent(repoUrl, targetDirectory, filesToValidate);
            } catch (IOException ex) {
                log.error("Could not delete directory {} before cloning repository {}", targetDirectory.getAbsolutePath(), repoUrl,
                        ex);
                return false;
            } catch (InvalidRemoteException ex) {
                log.error("Cannot found repository {}.", repoUrl, ex);
                return false;
            } catch (RefNotFoundException ex) {
                log.error("Invalid ref was set: {}.", repoUrl, ex);
                return false;
            } catch (GitAPIException ex) {
                log.error("Git exception while cloning repository {}", repoUrl, ex);
                attempts++;
                if (attempts <= maxAttempts) {
                    log.info("Wainting " + waiting / 1000 + " seconds.");
                    try {
                        Thread.sleep(waiting);
                    } catch (InterruptedException e) {
                        // swallow
                    }
                    waiting *= 2;
                }
            }
        }
        log.error("No more attemtps to try cloning of git repository. Cloning was UNSUCCESFUL.");
        return false;
    }

    private static Git clone(final String repoUrl, final String ref, final File targetDirectory)
            throws IOException, GitAPIException {
        log.info("Starting a potentially long running operation: cloning a git repository. Please be patient.");
        if (ref == null) {
            log.info("Cloning repository {} to directory {}", repoUrl, targetDirectory.getCanonicalPath());
        } else {
            log.info("Cloning repository {} (ref {}) to directory {}", repoUrl, ref, targetDirectory.getCanonicalPath());
        }

        return Git.cloneRepository().setURI(repoUrl).setDirectory(targetDirectory).call();
    }

    private static void checkout(final String ref, final Git git) throws GitAPIException {
        if (ref != null) {
            try {
                git.checkout().setCreateBranch(true).setName(ref).setStartPoint(ref).call();
            } catch (RefNotFoundException ex) {
                git.checkout().setCreateBranch(true).setName(ref).setStartPoint("origin/" + ref).call();
            } catch (RefAlreadyExistsException ex) {
                // ref already checked out
            }
        }
    }

    private static boolean expectedFilesArePresent(final String repoUrl, final File targetDirectory,
                                                   final List<String> filesToValidate) {
        boolean expectedFilesArePresent = true;

        for (String filename : filesToValidate) {
            if (!new File(targetDirectory, filename).exists()) {
                log.error("Cloning of repository {} seems to succeeded, but required file {} was not found", repoUrl, filename);
                expectedFilesArePresent = false;
            }
        }

        return expectedFilesArePresent;
    }
}
