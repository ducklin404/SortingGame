package group10.client.controller;

import group10.client.network.ClientConnection;
import group10.client.ui.LoginScreen;
import group10.client.ui.PlayerListView;
import group10.client.ui.GameScreen;
import group10.common.Message;
import group10.common.MessageType;
import group10.common.Protocol;
import org.json.JSONArray;
import org.json.JSONObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;

import java.util.HashMap;
import java.util.Map;

public class PlayerListController {
    private final PlayerListView view;
    private final Stage stage;
    private final ClientConnection client;
    private volatile boolean listening;
    private Thread listenerThread;
    private final String currentUserId;
    private Alert currentInviteAlert;
    private String currentInviteId;

    public PlayerListController(PlayerListView view, Stage stage, ClientConnection client, String currentUserId) {
        this.view = view;
        this.stage = stage;
        this.client = client;
        this.currentUserId = currentUserId;
        startListening();
    }

    public void fetchPlayerList(String userId) {
        view.showMessage("Đang chờ danh sách người chơi từ server...");
    }

    public void logout(String userId) {
        try {
            Message req = new Message(MessageType.LOGOUT);
            req.put("userId", userId);
            client.sendMessage(req);
            client.close();
            stopListening();

            stage.setScene(new Scene(new LoginScreen(stage).getRoot(), 500, 400));
        } catch (Exception e) {
            view.showMessage("⚠️ Lỗi khi đăng xuất!");
        }
    }

    private void startListening() {
        listening = true;
        listenerThread = new Thread(() -> {
            while (listening) {
                try {
                    Message msg = client.receiveMessage();
                    if (msg == null) {
                        break;
                    }
                    if (msg.getType() == MessageType.ONLINE_LIST) {
                        JSONObject payload = msg.getPayloadAsObject();
                        Object playersObj = payload.opt("players");
                        if (playersObj instanceof JSONArray arr) {
                            ObservableList<Map<String, String>> data = FXCollections.observableArrayList();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject p = arr.getJSONObject(i);
                                Map<String, String> row = new HashMap<>();
                                String display = p.optString("displayName", p.optString("username", ""));
                                String pid = p.optString("playerId", "");
                                if (pid.equals(currentUserId)) {
                                    continue;
                                }
                                row.put("display_name", display);
                                row.put("status", p.optString("status", ""));
                                row.put("player_id", pid);
                                data.add(row);
                            }
                            javafx.application.Platform.runLater(() -> {
                                view.setPlayers(data);
                                view.showMessage("✅ Danh sách cập nhật");
                            });
                        }
                    } else if (msg.getType() == MessageType.INVITE_RESPONSE) {
                        JSONObject payload = msg.getPayloadAsObject();
                        String status = payload.optString("status", "");
                        if ("REJECTED".equals(status)) {
                            javafx.application.Platform.runLater(() -> view.showMessage("❌ Lời mời bị từ chối"));
                        }
                    } else if (msg.getType() == MessageType.MATCH_START) {
                        JSONObject payload = msg.getPayloadAsObject();
                        String matchId = payload.optString("match_id", "");
                        String opponentName = payload.optString("opponent_name", "Đối thủ");
                        javafx.application.Platform.runLater(() -> {
                            GameScreen game = new GameScreen(stage, matchId, opponentName, client);
                            stage.setScene(new Scene(game.getRoot(), 600, 450));
                        });
                    } else if (msg.getType() == MessageType.INVITE) {
                        JSONObject payload = msg.getPayloadAsObject();
                        String inviteId = payload.optString("invite_id", "");
                        String fromName = payload.optString("from_display_name", payload.optString("from_username", "Đối thủ"));
                        javafx.application.Platform.runLater(() -> {
                            if (currentInviteAlert != null) {
                                currentInviteAlert.close();
                            }
                            currentInviteId = inviteId;
                            currentInviteAlert = new Alert(Alert.AlertType.CONFIRMATION);
                            currentInviteAlert.setTitle("Lời mời thách đấu");
                            currentInviteAlert.setHeaderText(fromName + " thách đấu bạn");
                            currentInviteAlert.setContentText("Chấp nhận lời mời?");
                            ButtonType accept = new ButtonType("Chấp nhận");
                            ButtonType reject = new ButtonType("Từ chối");
                            currentInviteAlert.getButtonTypes().setAll(accept, reject, ButtonType.CANCEL);
                            Button acceptBtn = (Button) currentInviteAlert.getDialogPane().lookupButton(accept);
                            Button rejectBtn = (Button) currentInviteAlert.getDialogPane().lookupButton(reject);
                            acceptBtn.setOnAction(e -> {
                                try {
                                    Message resp = new Message(MessageType.INVITE_RESPONSE);
                                    resp.put("invite_id", inviteId);
                                    resp.put("status", Protocol.STATUS_ACCEPTED);
                                    client.sendMessage(resp);
                                } catch (Exception ignored) {}
                                currentInviteAlert.close();
                                currentInviteAlert = null;
                                currentInviteId = null;
                            });
                            rejectBtn.setOnAction(e -> {
                                try {
                                    Message resp = new Message(MessageType.INVITE_RESPONSE);
                                    resp.put("invite_id", inviteId);
                                    resp.put("status", Protocol.STATUS_REJECTED);
                                    client.sendMessage(resp);
                                } catch (Exception ignored) {}
                                currentInviteAlert.close();
                                currentInviteAlert = null;
                                currentInviteId = null;
                            });
                            currentInviteAlert.show();
                        });
                    } else if (msg.getType() == MessageType.INVITE_EXPIRED) {
                        JSONObject payload = msg.getPayloadAsObject();
                        String text = payload.optString("message", "Lời mời đã hết hạn");
                        String inviteId = payload.optString("invite_id", "");
                        javafx.application.Platform.runLater(() -> {
                            view.showMessage("⌛ " + text);
                            Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
                            alert.setHeaderText("Hết hạn");
                            alert.showAndWait();
                            if (currentInviteAlert != null && (currentInviteId == null || currentInviteId.equals(inviteId))) {
                                currentInviteAlert.close();
                                currentInviteAlert = null;
                                currentInviteId = null;
                            }
                        });
                    }
                } catch (Exception ignored) {
                    break;
                }
            }
        }, "player-list-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void stopListening() {
        listening = false;
        if (listenerThread != null) listenerThread.interrupt();
    }

    public void challengeSelected() {
        try {
            Map<String, String> selected = view.getSelectedPlayer();
            if (selected == null) {
                view.showMessage("⚠️ Vui lòng chọn đối thủ để thách đấu");
                return;
            }
            String toPlayerId = selected.get("player_id");
            if (toPlayerId == null || toPlayerId.isBlank()) {
                view.showMessage("⚠️ Lỗi: không tìm thấy ID đối thủ");
                return;
            }
            Message invite = new Message(MessageType.INVITE);
            invite.put("to_player_id", toPlayerId);
            client.sendMessage(invite);
            view.showMessage("📨 Đã gửi lời mời, chờ phản hồi...");
        } catch (Exception e) {
            view.showMessage("⚠️ Không thể gửi lời mời");
        }
    }

    public void challengePlayer(String toPlayerId, String displayName) {
        try {
            if (toPlayerId == null || toPlayerId.isBlank()) {
                view.showMessage("⚠️ Lỗi: không tìm thấy ID đối thủ");
                return;
            }
            Message invite = new Message(MessageType.INVITE);
            invite.put("to_player_id", toPlayerId);
            client.sendMessage(invite);
            view.showMessage("📨 Đã gửi lời mời tới " + displayName + ", chờ phản hồi...");
        } catch (Exception e) {
            view.showMessage("⚠️ Không thể gửi lời mời");
        }
    }
}