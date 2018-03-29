package jenkinsci.plugins.telegrambot;

import jenkinsci.plugins.telegrambot.component.*;
import jenkinsci.plugins.telegrambot.config.GlobalConfiguration;
import jenkinsci.plugins.telegrambot.telegram.TelegramBotRunner;
import jenkinsci.plugins.telegrambot.users.Subscribers;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;

/**
 * Since the implementation of the variable resolving and
 * the sending messages in the Builder is the same for the Recorder,
 * it is moved to the delegate method.
 */
public class TelegramBotDelegate {
    private static final Logger LOGGER = Logger.getLogger(TelegramBotDelegate.class.getName());

    private String message;

    TelegramBotDelegate(String message) {
        this.message = message;
    }

    public void perform(Run<?, ?> run, FilePath filePath, Launcher launcher, TaskListener taskListener)
            throws IOException, InterruptedException {

        MiddlewareController middlewareController = new DefaultMiddlewareController();
        middlewareController
                .linkWith(new NativeJenkinsMiddleware(run.getEnvironment(taskListener)))
                .linkWith(new KeyPhraseMiddleware("READ_FROM_FILE"));

        String logMessage = new ExtensionMessageTransformer(middlewareController).transform(message);

        GlobalConfiguration config = GlobalConfiguration.getInstance();

        try {
            Subscribers.getInstance().getApprovedUsers()
                    .forEach(user -> TelegramBotRunner.getInstance().getBotThread()
                            .getBot().sendMessage(user.getId(), logMessage));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while sending the message", e);
        }
        
        if (config.shouldLogToConsole()) {
            taskListener.getLogger().println(logMessage);
        }

    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
