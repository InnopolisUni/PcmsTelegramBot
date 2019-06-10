import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;

public class PcmsBot extends TelegramLongPollingBot {

    final HashMap<Long, User> chats = new HashMap<Long, User>();
    private final String botUsername;
    private final String botToken;

    public PcmsBot(String name, String token) {
        botUsername = name;
        botToken = token;
    }

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            SendMessage message = new SendMessage().setChatId(chatId);

            User user = chats.get(chatId);
            if (user == null) {
                user = new User();
                user.chatId = chatId;
            }

            if (message_text.startsWith("/login")) {
                String[] parts = message_text.split(" ");
                if (parts.length == 3) {
                    user.login = parts[1];
                    user.pass = parts[2];
                    //todo: check if he can login;
                    synchronized (chats) {
                        chats.put(chatId, user);
                    }
                    message.setText("Starting to watch undefined and failed failedJobs");
                    System.out.println("LOGIN: " + user.login);
                } else {
                    message.setText("Sorry. Provide your login and password by typing /login <user> <pass>");
                    System.out.println("LOGIN FAILED: " + message_text);
                }

            } else {
                //todo: other commands
                if (chats.containsKey(chatId)) {
                    message.setText("Already watching your failedJobs");
                } else {
                    message.setText("Provide your login and password by typing /login <user> <pass>");
                }
            }

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public String getBotUsername() {
        return botUsername;
    }

    public String getBotToken() {
        return botToken;
    }

}