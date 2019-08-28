package pcms.telegram.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pcms.telegram.bot.domain.User;
import pcms.telegram.bot.repos.UserRepo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class PcmsBot extends TelegramLongPollingBot {
    @Autowired
    UserRepo userRepo;

    //chatId -> List<User>
    final HashMap<Long, List<User>> chats = new HashMap<Long, List<User>>();
    private String botUsername;
    private String botToken;

    public PcmsBot() {
        Main.bot = this;
    }

    public void init(String name, String token) {
        botUsername = name;
        botToken = token;
        Iterable<User> users = userRepo.findAll();
        for (User u : users) {
            List<User> userList = chats.get(u.getChatId());
            if (userList == null) {
                userList = new ArrayList<User>();
                synchronized (chats) {
                    chats.put(u.getChatId(), userList);
                }
            }
            userList.add(u);
        }
    }

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            SendMessage message = new SendMessage().setChatId(chatId);

            User user = new User();
            user.setChatId(chatId);

            if (message_text.startsWith("/login")) {
                String[] parts = message_text.split(" ");
                if (parts.length == 3) {
                    user.setLogin(parts[1]);
                    user.setPass(parts[2]);
                    user.setWatchRuns(RunListWatcher.canLogin(user.getLogin(), user.getPass()));
                    user.setWatchQuestions(QuestionsWatcher.canLogin(user.getLogin(), user.getPass()));

                    if (user.isWatchRuns() || user.isWatchQuestions()) {
                        List<User> userList = chats.get(chatId);
                        if (userList == null) {
                            userList = new ArrayList<User>();
                            synchronized (chats) {
                                chats.put(chatId, userList);
                            }
                        }
                        int index = userList.indexOf(user);
                        if (index != -1) {
                            User orig = userList.get(index);
                            orig.setWatchRuns(user.isWatchRuns());
                            orig.setWatchQuestions(user.isWatchQuestions());
                            user = orig;
                        } else {
                            synchronized (chats) {
                                userList.add(user);
                            }
                        }
                        userRepo.save(user);
                        message.setText(user.toString() + " Type /logout <user> <pass> to stop");
                        System.out.println("LOGIN: " + user.toString());
                    } else {
                        message.setText("Sorry, couldn't login. Provide your login and password by typing /login <user> <pass>");
                        System.out.println("LOGIN FAILED: " + user.getLogin());
                    }
                } else {
                    message.setText("Sorry. Provide your login and password by typing /login <user> <pass>");
                    System.out.println("LOGIN FAILED: " + message_text);
                }

            } else if (message_text.startsWith("/logout")) {
                String[] parts = message_text.split(" ");
                if (parts.length == 1) {
                    userRepo.deleteByChatId(user.getChatId());
                    message.setText("Stopped watching");
                    System.out.println("LOGOUT: " + User.getLoginList(chats.get(chatId)));
                    synchronized (chats) {
                        chats.remove(chatId);
                    }
                } else if (parts.length == 3) {
                    user.setLogin(parts[1]);
                    user.setPass(parts[2]);
                    List<User> userList = chats.get(chatId);
                    boolean found;
                    synchronized (chats) {
                        found = userList.remove(user);
                        if (userList.isEmpty()) {
                            chats.remove(chatId);
                        }
                    }
                    if (found) {
                        userRepo.deleteByChatIdAndLoginAndPass(user.getChatId(), user.getLogin(), user.getPass());
                        message.setText("Stopped watching user " + user.getLogin());
                        System.out.println("LOGOUT: " + user.getLogin());
                    } else {
                        message.setText("Not found login and password pair: " + user.getLogin() + " " + user.getPass());
                    }
                } else {
                    message.setText("Type /logout to stop watching all users or /logout <user> <pass> for one user");
                }
            } else {
                //todo: other commands
                if (chats.containsKey(chatId)) {
                    StringBuilder sb = new StringBuilder("Watching your users:\n");
                    for (User i : chats.get(chatId))
                        sb.append(i.toString()).append("\n");
                    message.setText(sb.toString());
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