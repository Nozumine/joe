package Danganronpa;

import Danganronpa.Helpers.Discord.Capsule;
import Danganronpa.Helpers.GameItems.*;
import Danganronpa.Helpers.Other.LoginUtil;
import Danganronpa.Helpers.Other.Settings;
import Danganronpa.Helpers.Other.SimpleFunctions;
import Danganronpa.Helpers.Other.Spreadsheet;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class Controller implements Initializable {
    //==Global Items==//
    public static final ArrayList<Tag> TAGS = new ArrayList<>();
    private static final ArrayList<GameMode> GAME_MODES = new ArrayList<>();
    private static final HashMap<String, ArrayList<Role>> ROLES = new HashMap<>();
    private static final DataFormat USER_LIST = new DataFormat("UserList");
    private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
    private static final ObservableList<Player> userList = FXCollections.observableArrayList();
    //private static JDA jda;
    private static Guild mainGuild;
    private final ArrayList<TextField> customGameField = new ArrayList<>();
    private final ArrayList<SuperRole> superRoles = new ArrayList<>();
    private Timeline timer = new Timeline();
    private Media gong;
    private int curTime;

    //==Screen Items==//
    public TextField customTimeField, customRollField, standardGameField, userSearchField;
    public VBox customGameVBox;
    public FlowPane rngFlow, timeFlow;
    public Button confirmButton;
    public ToggleButton playToggle;
    public ProgressBar dmProgress;
    public ContextMenu editRolesMenu;
    public MenuItem removeRoleMenuItem, updateBar;
    public Menu addRoleMenu, tagMenu;
    public ListView<String> roleList;
    public ListView<Player> displayedUserList, playerList, modsList;
    public ComboBox<GameMode> gamemodeBox;
    public Label rollLabel, statusLabel, timerLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Build a new authorized API client service.
        try {
            //==Github Updates==//
            LOGGER.info("Checking for Updates...");
            SimpleFunctions.getGithub(true);
            updateBar.setOnAction(e -> SimpleFunctions.getGithub(false));

            //==Main Settings==//
            LOGGER.info("Checking for 'config.properties' file");
            File f = new File("./config.properties");
            if(f.createNewFile()){
                LOGGER.warn("Couldn't find the `config.properties` file... New file will be created.");
                for(int x = 0; x < Settings.FIELDS.length; x++) Settings.getInst().getConfig().setProperty(Settings.FIELDS[x], "");
                while (Settings.getInst().getConfig().getProperty(Settings.FIELDS[0],"").equals("")) quarryInfoAlert();
                Settings.getInst().getConfig().store(new FileOutputStream(f),null);
            }
            else {
                LOGGER.info("Found the 'config.properties' file!");
                Settings.getInst().getConfig().load(new FileInputStream(f));
                for(int x = 0; x < Settings.FIELDS.length; x++){
                    if(Settings.getInst().getProperty(Settings.FIELDS[x]) == null) Settings.getInst().getConfig().setProperty(Settings.FIELDS[x], "");
                }
                while (Settings.getInst().getProperty(Settings.FIELDS[0],"").equals("")) {
                    LOGGER.warn("config.properties file is missing critical information!!");
                    quarryInfoAlert();
                    Settings.getInst().getConfig().store(new FileOutputStream(f), null);
                }
            }

            //==Sheet Settings==//
            LOGGER.info("Accessing Settings");
            List<List<Object>> sheetVals = Spreadsheet.getInstance().getRange(Settings.getInst().getSettingsRange(),"COLUMNS");
            if(sheetVals.isEmpty()) {
                LOGGER.error("No Spreadsheet Settings Found!");
                System.exit(0);
            }
            Settings.getInst().addSettings(sheetVals.get(0));

            //==User Login==//
            LOGGER.info("Accessing Login Information");
            buildList(Settings.getInst().getLoginRange(), "Hosts");
            while (true) {
                if(Settings.getInst().getUsername().equals("") || Settings.getInst().getPassword().equals("")) {
                    login();
                    Settings.getInst().getConfig().store(new FileOutputStream(f), null);
                }
                //If Login Correct and User was gathered from spreadsheet
                if(LoginUtil.getInst().isUsernameTaken(Settings.getInst().getUsername())) {
                    if(!LoginUtil.getInst().isLoginCorrect(Settings.getInst().getUsername(), Settings.getInst().getPassword())) {
                        Settings.getInst().setPassword("");
                        LOGGER.warn("Username or Password is incorrect...");
                    }
                    else break;
                }
                //Register User Otherwise
                else {
                    LoginUtil.getInst().registerUser(Settings.getInst().getUsername(), Settings.getInst().getPassword());
                    Spreadsheet.getInstance().appendSpreadsheet("Login",Collections.singletonList(LoginUtil.getInst().getUserAsList(Settings.getInst().getUsername())));
                    LOGGER.info("New Login Created; Awaiting Approval!");
                    break;
                }
            }
            if(!LoginUtil.getInst().isUserActiveHost(Settings.getInst().getUsername())) {
                LOGGER.error("You have not been set as an active user by the Spreadsheet host");
                SimpleFunctions.createBasicAlert(Settings.getInst().getLogo(), Alert.AlertType.WARNING,"New Account Detected",
                        "Your account HAS NOT been approved yet!",
                        "Please contact the spreadsheet host before using the DMMT!").showAndWait();
                System.exit(0);
            }

            //==List Building==//
            buildList(Settings.getInst().getRolesRange(),"Roles");
            buildList(Settings.getInst().getTagsRange(),"Tags");
            buildList(Settings.getInst().getGameModeRange(),"Game Modes");

            //==Discord Setup==// (Updated for JDA 4.2.0)
            LOGGER.info("Booting Discord...");
            JDABuilder.createDefault(Settings.getInst().getDiscordToken(), GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_BANS, GatewayIntent.GUILD_INVITES,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES)
            .setBulkDeleteSplittingEnabled(false).setAutoReconnect(true).addEventListeners(new Capsule())
            .setMemberCachePolicy(MemberCachePolicy.ALL).setActivity(Activity.watching("over "+ Settings.getInst().getUsername()+"'s class..."))
            .build();
            //==Audio==//
            gong = new Media(Main.class.getResource("/Media/timer.wav").toURI().toString());
        } catch (GeneralSecurityException | IOException | URISyntaxException e) {
            LOGGER.error("Exception in 'Initialize' Method");
        }

        //==Other Initialisations==//
        gamemodeBox.getItems().addAll(GAME_MODES);
        FilteredList<Player> filteredUserList = new FilteredList<>(userList, s -> true);
        SortedList<Player> sortedUserList = new SortedList<>(filteredUserList, Comparator.comparing(t -> t.getPreferredName().toLowerCase()));
        displayedUserList.setItems(sortedUserList);
        listViewSetup(displayedUserList, false);
        listViewSetup(playerList, true);
        listViewSetup(modsList, true);
        //Search Bar
        userSearchField.textProperty().addListener((observable, oldVal, newVal) -> filteredUserList.setPredicate(i -> {
            //Zero case
            if (newVal == null || newVal.isEmpty()) return true;
            //Test Cases (separate as more if's if needed)
            if (i.getName().toUpperCase().contains(newVal.toUpperCase())) return true;
            return i.getPreferredName().toUpperCase().contains(newVal.toUpperCase());
        }));

        //==Dynamic Game Fields & Role Menus==//
        LOGGER.info("Adding Custom Fields and Updating Menus");
        Object[] cate = ROLES.keySet().toArray();
        Arrays.sort(cate);
        for(Object item: cate){
            //Custom game Fields
            TextField addable = new TextField();
            addable.setPromptText("# "+item.toString());
            addable.setFont(new Font(15));
            addable.setAlignment(Pos.CENTER);
            VBox.setMargin(addable, new Insets(2,10,2,0));
            customGameField.add(addable);
            customGameVBox.getChildren().add(customGameVBox.getChildren().size()-2,addable);
            //Role Menus
            Menu add = new Menu(item.toString());
            add.setMnemonicParsing(false);
            ArrayList<Role> roles = ROLES.get(item.toString());
            for(int x = 0; x < roles.size(); x++){
                int cur = x;
                MenuItem role = new MenuItem(roles.get(x).toString());
                role.setMnemonicParsing(false);
                role.setOnAction(event -> menuAddRole(roles.get(cur)));
                add.getItems().add(role);
            }
            addRoleMenu.getItems().add(add);
        }

        //==Tag Menu==//
        for(Tag item: TAGS){
            MenuItem add = new MenuItem(item.toString());
            add.setOnAction(event -> menuAddTag(item));
            tagMenu.getItems().add(add);
        }
        //Clear option
        MenuItem clear = new MenuItem("Clear");
        clear.setOnAction(event -> menuRemoveTag());
        tagMenu.getItems().add(clear);

        //==Repeating Buttons==//
        LOGGER.info("Building Buttons");
        buildButtons("RNG", rngFlow, 4,6,8,10,12,20);
        buildButtons("TIME", timeFlow, 1,2,4,6,8,10);

        //==Finalise==//
        statusUpdate();
        LOGGER.info("Waiting for Discord Connection...");
    }
    public static void afterDiscordStartup(Guild guild) {
        mainGuild = guild;
        buildList(Settings.getInst().getPlayerRange(), "Players");
        checkNewUsers();
        LOGGER.info("Finished Initialization");
    }
    private void quarryInfoAlert() {
        // Create the custom dialog.
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("First Run Setup: Spreadsheet ID");
        dialog.setHeaderText("Please enter the following credentials for the program to run");
        dialog.initModality(Modality.APPLICATION_MODAL);

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(this.getClass().getResource("/Media/logo.png").toString()));

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(loginButtonType);

        TextField sheetID = new TextField();
        sheetID.setPromptText("sheetID");
        sheetID.setText(Settings.getInst().getProperty("sheetID"));

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Google Sheet ID:"),0,0); grid.add(sheetID,1,0);

        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        sheetID.textProperty().addListener((observable, oldValue, newValue) -> loginButton.setDisable(newValue.trim().isEmpty()));
        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(sheetID::requestFocus);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> (dialogButton == loginButtonType)?sheetID.getText():null);
        dialog.showAndWait().ifPresent(ret -> Settings.getInst().setSheetID(ret));
    }
    private void login(){
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("First Run Setup: Login Information");
        dialog.setHeaderText("Login or Register an account to access the spreadsheet:");
        dialog.initModality(Modality.APPLICATION_MODAL);

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(this.getClass().getResource("/Media/logo.png").toString()));

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE),
        registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, registerButtonType);

        // Create the username and password labels and fields.
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        username.setPromptText("username"); username.setText(Settings.getInst().getUsername());
        password.setPromptText("password"); password.setText(Settings.getInst().getPassword());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Username:"),0,0); grid.add(username,1,0);
        grid.add(new Label("Password:"),0,1); grid.add(password,1,1);

        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType),
             registerButton = dialog.getDialogPane().lookupButton(registerButtonType);
        boolean toggle = LoginUtil.getInst().isUsernameTaken(username.getText());
        loginButton.setDisable(!toggle); registerButton.setDisable(toggle);

        // Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            if(LoginUtil.getInst().isUsernameTaken(newValue)){
                registerButton.setDisable(true);
                loginButton.setDisable(false);
            }
            else {
                registerButton.setDisable(oldValue.trim().isEmpty() || newValue.trim().isEmpty());
                loginButton.setDisable(true);
            }
        });
        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(username::requestFocus);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(b -> (b == registerButtonType || b == loginButtonType)? new Pair<>(username.getText(), password.getText()) : null);
        dialog.showAndWait().ifPresent(ret -> {
            Settings.getInst().setUsername(ret.getKey());
            Settings.getInst().setPassword(ret.getValue());
        });
    }

    //==Update Spreadsheet==//
    public static void update(Member m){
        Player s = new Player(m);
        for(Player plr : userList) if(plr.getID().equals(s.getID())) return;
        userList.add(s);
        Spreadsheet.getInstance().appendSpreadsheet("Players", Collections.singletonList(s.getPlayerSheetVariable()));
        LOGGER.info("A new user joined just joined the server! They have been added to the user list!");
    }
    public static void checkNewUsers(){
        boolean found;
        ArrayList<Player> temp = new ArrayList<>();
        for(Member m: mainGuild.getMembers()){
            found = false;
            if(m.getUser().isBot()) continue;
            for(Player s: userList) {
                if(s.getID().equals(m.getId())) {
                    found = true;
                    break;
                }
            }
            if(!found) temp.add(new Player(m));
        }
        if(temp.isEmpty()) return;
        userList.addAll(temp);
        List<List<Object>> toAppend = new ArrayList<>();
        for(Player s: temp) toAppend.add(s.getPlayerSheetVariable());
        Spreadsheet.getInstance().appendSpreadsheet("Players", toAppend);
        LOGGER.info("{} new users have been added to the user list!", toAppend.size());
    }

    //==Game Stuff==//
    public void startGame(){
        StringBuilder fileOut = new StringBuilder();
        ArrayList<Player> playing = new ArrayList<>(playerList.getItems());
        Collections.shuffle(playing);
        //Guaranteed that both are same length
        ArrayList<PackagedUser> packagedUsers = new ArrayList<>();
        for(int x = 0; x < playing.size(); x++) packagedUsers.add(new PackagedUser(playing.get(x), superRoles.get(x)));
        for(PackagedUser s: packagedUsers){
            sendDM(s.getUser(),"**"+Settings.getInst().getRoleTitlePrefix()+" "+s.getSuperRole().getRole()+"**:\n"+s.getSuperRole().getRole().getDescription());
            for(Tag t: s.getSuperRole().getTags()) {
                if(!t.isEmpty()) sendDM(s.getUser(),"**"+t.getName()+"**:\n"+t.getDescription());
            }
            sendDM(s.getUser(),Settings.getInst().getExtraMessage());
            fileOut.append(s.toString()).append("\r\n");
            updateUser(s.getUser());
        }

        //Sends selected mods the list of those playing with their roles
        for(Player u: modsList.getItems()) sendDM(u, "```\n"+fileOut.toString()+"```");

        //generates file with all player's roles as backup
        try {
            String[] headerText = new String[]{"Here?","User Name","Tag","Role","Extra Notes","","Day 1","Day 2","Day 3","Day 4","Day 5","Day 6","Day 7","Day 8"};
            FileOutputStream output = new FileOutputStream(new File("./Game.xls").getAbsoluteFile());
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("Game ("+LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM, HH-mm"))+")");

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont(), bodyFont = workbook.createFont();
            headerFont.setFontName("TimesRoman");
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short)14);
            bodyFont.setFontName("TimesRoman");
            bodyFont.setFontHeightInPoints((short)12);

            CellStyle headStyle = workbook.createCellStyle(), bodyStyle = workbook.createCellStyle();
            headStyle.setFont(headerFont);
            bodyStyle.setFont(bodyFont);

            HSSFRow rowHead = sheet.createRow(0);
            for(int x = 0; x < headerText.length; x++) {
                Cell created = rowHead.createCell(x);
                created.setCellValue(headerText[x]);
                created.setCellStyle(headStyle);
            }

            for(int x = 0; x < packagedUsers.size(); x++){
                HSSFRow row = sheet.createRow(x+1);
                String[] data = new String[]{
                        "✓",
                        packagedUsers.get(x).getUser().toString(),
                        packagedUsers.get(x).getSuperRole().getFullTagsAsString(),
                        packagedUsers.get(x).getSuperRole().getRole().toString()
                };
                for(int y = 0; y < data.length;  y++){
                    Cell created = row.createCell(y);
                    created.setCellValue(data[y]);
                    created.setCellStyle(bodyStyle);
                }
            }
            for(int i = 0; i < headerText.length; i++) sheet.autoSizeColumn(i);

            workbook.write(output);
            output.close();
            workbook.close();
            LOGGER.info("Spreadsheet Created");
        } catch(IOException e) {
            LOGGER.error("Exception in 'StartGame' Method");
        }
        confirmButton.setDisable(true);
    }
    public void customRNG(){
        if(!SimpleFunctions.fieldHasDigits(customRollField)) customRollField.setText("0");
        rollLabel.setText(SimpleFunctions.rollTheDice(Integer.parseInt(customRollField.getText())));
    }

    //==Countdown Timers==//
    public void customTimer(){
        if(SimpleFunctions.fieldHasDigits(customTimeField)) startTimer(SimpleFunctions.getTimeInSeconds(0, Integer.parseInt(customTimeField.getText())));
        else if(customTimeField.getText().matches("[0-9]+([:])[0-9]+")) {
            String[] items = customTimeField.getText().split(":");
            startTimer(SimpleFunctions.getTimeInSeconds(Integer.parseInt(items[0]), Integer.parseInt(items[1])));
        }
    }
    public void stopTimer(){
        timer.stop();
        curTime = 0;
        timerLabel.setText(SimpleFunctions.displayTime(curTime));
        pauseResumeTimer();
    }
    public void pauseResumeTimer(){
        if(timer.getStatus() == Animation.Status.STOPPED && curTime == 0) playToggle.setSelected(false);
        playToggle.setText((playToggle.isSelected())?("\u23F8"):("▶"));
        if(playToggle.isSelected()) timer.play();
        else timer.pause();
    }
    private void startTimer(int timeSec){
        if(playToggle.isSelected()) playToggle.setSelected(false);
        curTime = timeSec;
        if(timer.getStatus() == Animation.Status.RUNNING) timer.stop();
        MediaPlayer timeEnd = new MediaPlayer(gong);
        timeEnd.setVolume(0.5);
        timerLabel.setText(SimpleFunctions.displayTime(curTime));
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if(curTime-- == 1) timeEnd.play();
            timerLabel.setText(SimpleFunctions.displayTime(curTime));
        }));
        timer.setCycleCount(timeSec);
    }

    //==Role Generation==//
    public void customGame(){
        ArrayList<String> heir = new ArrayList<>();
        for (TextField curField : customGameField) {
            if (!SimpleFunctions.fieldHasDigits(curField)) continue;
            ArrayList<Role> size = ROLES.get(curField.getPromptText().substring(2));
            if (size == null) continue;
            int len = Integer.parseInt(curField.getText());
            if (len > size.size()) {//if overflow
                len = size.size();
                curField.setText(len + "");
            }
            //Build exact length Hierarchy
            for (int y = 0; y < len; y++) heir.add(curField.getPromptText().substring(2));
        }
        startGenerate(heir.size(), heir);
    }
    public void standardGame(){
        int len = 12;
        if(SimpleFunctions.fieldHasDigits(standardGameField)) len = Integer.parseInt(standardGameField.getText());
        else standardGameField.setText(""+len);
        startGenerate(len, new ArrayList<>(Arrays.asList(Settings.getInst().getHierarchy())));
    }
    public void clearRoles(){
        roleList.getItems().clear();
        superRoles.clear();
        statusUpdate();
    }
    public void menuRequest(ContextMenuEvent event){
        editRolesMenu.show(roleList, event.getScreenX(), event.getScreenY());
        removeRoleMenuItem.setDisable(roleList.getSelectionModel().isEmpty());
        tagMenu.setDisable(roleList.getSelectionModel().isEmpty());
        event.consume();
    }
    public void menuRemoveRole(){
        superRoles.remove(roleList.getItems().indexOf(roleList.getSelectionModel().getSelectedItem()));
        roleList.getItems().remove(roleList.getSelectionModel().getSelectedItem());
        statusUpdate();
    }
    private void startGenerate(int len, ArrayList<String> hierarchy){
        if(gamemodeBox.getValue() == null) gamemodeBox.setValue(GAME_MODES.get(0));
        if(len < gamemodeBox.getValue().getCalculatedTagsSize(len)) return;
        if(len > 0 && len <= ROLES.get("All").size()) generateGame(hierarchy, len, gamemodeBox.getValue().performActions(len));
    }
    private void generateGame(ArrayList<String> heir, int n, ArrayList<Tag> tags){
        superRoles.clear();
        ArrayList<Role> roleSelection = new ArrayList<>();
        for(int ran, x = 0; x < n; x++) {
            String type = heir.get(x%heir.size()); //Loop through hierarchy order.
            if(ROLES.get(type) == null) {
                LOGGER.error("Error in Role Types. Is the hierarchy up to date?");
                return;
            }
            ArrayList<Role> sub = ROLES.get(type);
            //Pick Random role from category
            ran = (int)(Math.random()*sub.size());
            roleSelection.add(sub.get(ran));
        }
        //Create Students
        Collections.shuffle(tags);
        for(int x = 0; x < n; x++){
            if(tags.get(x).getName().equals("")) superRoles.add(new SuperRole(roleSelection.get(x)));
            else superRoles.add(new SuperRole(roleSelection.get(x),tags.get(x)));
        }

        //Generate Tree
        roleList.getItems().clear();
        for(SuperRole s: superRoles) roleList.getItems().add(s.getTagsAsString()+s.getRole());
        statusUpdate();
    }
    private void menuAddRole(Role...roles){
        for(Role r: roles){
            /*TreeItem<String> temp = makeBranch(r.toString(), roleTree.getRoot());
            makeBranch(r.getDescription(),temp);*/
            SuperRole p = new SuperRole(r);
            superRoles.add(p);
            roleList.getItems().add(p.getTagsAsString()+p.getRole());
        }
        if(gamemodeBox.getValue() == null || !gamemodeBox.getValue().getName().equals("Custom")) gamemodeBox.setValue(gamemodeBox.getItems().get(gamemodeBox.getItems().size()-1));
        statusUpdate();
    }
    private void menuAddTag(Tag t){
        SuperRole s = superRoles.get(roleList.getItems().indexOf(roleList.getSelectionModel().getSelectedItem()));
        s.addTags(t);
        roleList.getItems().set(roleList.getSelectionModel().getSelectedIndex(), s.getTagsAsString()+s.getRole());
        statusUpdate();
    }
    private void menuRemoveTag(){
        SuperRole s = superRoles.get(roleList.getItems().indexOf(roleList.getSelectionModel().getSelectedItem()));
        s.getTags().clear();
        roleList.getItems().set(roleList.getSelectionModel().getSelectedIndex(), s.getTagsAsString()+s.getRole());
        statusUpdate();
    }

    //==User Building==//
    public void clearCreated(){
        //Push back
        userList.addAll(modsList.getItems());
        userList.addAll(playerList.getItems());
        //Pop remaining
        playerList.getItems().clear();
        modsList.getItems().clear();
        //Sort list and update
        userSearchField.clear();
        statusUpdate();
    }
    private void statusUpdate(){
        double stat = 0;
        String text = "Status: ";
        if(roleList.getItems().isEmpty()) text += "(R?) ";
        else stat += 0.35;
        if(playerList.getItems().isEmpty()) text += "(P?) ";
        else stat += 0.35;
        if(!playerList.getItems().isEmpty() && !roleList.getItems().isEmpty()){
            if(playerList.getItems().size() == roleList.getItems().size()) {
                text += "Ready";
                stat += 0.3;
                confirmButton.setDisable(false);
            }
            else {
                double max = Math.max(playerList.getItems().size(), roleList.getItems().size());
                double temp = Math.abs(playerList.getItems().size() - roleList.getItems().size());
                text += "(P:"+playerList.getItems().size()+" | R:"+roleList.getItems().size()+")";
                stat += (0.3 * (1-(temp/max)));
                confirmButton.setDisable(true);
            }
        }
        else confirmButton.setDisable(true);
        statusLabel.setText(text);
        dmProgress.setProgress(stat);
    }
    private void listViewSetup(ListView<Player> listView, boolean basic){
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setOnDragDetected(event -> dragDetected(event, listView));
        listView.setOnDragOver(event -> dragOver(event, listView));
        listView.setOnDragDropped(event -> dragDropped(event, listView, basic));
        listView.setOnDragDone(event -> dragDone(event, listView, basic));
    }

    //--Drag And Drops--//
    private void dragDetected(MouseEvent event, ListView<Player> listView){
        // Make sure at least one item is selected
        int selectedCount = listView.getSelectionModel().getSelectedIndices().size();

        if (selectedCount == 0) {
            event.consume();
            return;
        }

        // Initiate a drag-and-drop gesture
        Dragboard dragboard = listView.startDragAndDrop(TransferMode.COPY_OR_MOVE);
        // Put the the selected items to the drag board
        ArrayList<Player> selectedItems = new ArrayList<>(listView.getSelectionModel().getSelectedItems());

        ClipboardContent content = new ClipboardContent();
        content.put(USER_LIST, selectedItems);

        dragboard.setContent(content);
        event.consume();
    }
    private void dragOver(DragEvent event, ListView<Player> listView){
        Dragboard dragboard = event.getDragboard();
        if (event.getGestureSource() != listView && dragboard.hasContent(USER_LIST)) event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        event.consume();
    }
    @SuppressWarnings("unchecked")
    private void dragDropped(DragEvent event, ListView<Player> listView, boolean basic){
        boolean dragCompleted = false;
        // Transfer the data to the target
        Dragboard dragboard = event.getDragboard();
        if(dragboard.hasContent(USER_LIST)) {
            ArrayList<Player> list = (ArrayList<Player>)dragboard.getContent(USER_LIST);
            if(basic){
                listView.getItems().addAll(list);
                listView.getItems().sort(Comparator.comparing(t -> t.getPreferredName().toLowerCase()));
            }
            else userList.addAll(list);
            // Data transfer is successful
            dragCompleted = true;
        }
        // Data transfer is not successful (DON'T SORT LIST VIEW HERE YOU BITCH)
        event.setDropCompleted(dragCompleted);
        event.consume();
    }
    private void dragDone(DragEvent event, ListView<Player> listView, boolean basic){
        TransferMode tm = event.getTransferMode();
        if (tm == TransferMode.MOVE) {
            // Get all selected items in a separate list to avoid the shared list issue
            List<Player> selectedList = new ArrayList<>(listView.getSelectionModel().getSelectedItems());
            // Clear the selection
            listView.getSelectionModel().clearSelection();
            // Remove items from the selected list
            if(basic) listView.getItems().removeAll(selectedList);
            else userList.removeAll(selectedList);
        }
        statusUpdate();
        event.consume();
    }

    //==Menu Bar==//
    public void getAbout(){
        SimpleFunctions.openGithubLink("/blob/master/README.md#about");
    }
    public void getHelp(){
        SimpleFunctions.openGithubLink("/blob/master/README.md#how-do-i-use-this-program");
    }

    //==Helpers==//
    private void sendDM(Player p, String message){
        Objects.requireNonNull(mainGuild.getMemberById(p.getID())).getUser().openPrivateChannel().queue(ch -> ch.sendMessage(message).queue());
    }
    private void updateUser(Player p){
        //FIXME The updated user isn't displayed instantly once the update is sent
        Member m = mainGuild.getMemberById(p.getID());
        if(m == null) return;
        int x = 2;
        for(List<Object> row: Spreadsheet.getInstance().getRange(Settings.getInst().getPlayerRange())){
            if(row.get(0).toString().equals(p.getID())){
                String name = m.getUser().getName();
                if(!row.get(1).toString().equals(name)){
                    row.set(1,name);
                    Spreadsheet.getInstance().updateSingleEntry("Players!A"+x+":P", Collections.singletonList(row));
                    p.setName(name);
                }
                break;
            }
            x++;
        }
    }
    private static void buildList(String range, String prompt){
        LOGGER.info("Loading {}", prompt);
        List<List<Object>> sheetVals = Spreadsheet.getInstance().getRange(range);
        if (!sheetVals.isEmpty()) {
            for (List<Object> row: sheetVals) {
                if(!row.isEmpty()) {
                    if(range.equals(Settings.getInst().getRolesRange())) {
                        String type = row.get(0).toString();
                        ROLES.putIfAbsent(type, new ArrayList<>());
                        ROLES.get(type).add(new Role(row));
                    }
                    else if(range.equals(Settings.getInst().getTagsRange())) TAGS.add(new Tag(row));
                    else if(range.equals(Settings.getInst().getGameModeRange())) GAME_MODES.add(new GameMode(row));
                    else if(range.equals(Settings.getInst().getPlayerRange())) userList.add(new Player(row));
                    else if(range.equals(Settings.getInst().getLoginRange())) LoginUtil.getInst().addUserToMaps(row);
                }
            }
        }
        //Add an ALL category and ignore Beta
        if(range.equals(Settings.getInst().getRolesRange())){
            ArrayList<Role> all = new ArrayList<>();
            for (String key: ROLES.keySet()){
                if(key.equalsIgnoreCase("Beta")) continue;
                all.addAll(ROLES.get(key));
            }
            all.sort(Comparator.comparing(Role::getName));
            ROLES.put("All", all);
        }
    }
    private void buildButtons(String type, FlowPane append, int...data){
        for(int d: data){
            Button but = new Button(""+d);
            switch (type){
                case "RNG":{
                    but.setFont(Font.font("System", FontWeight.BOLD, 42));
                    but.setOnMousePressed(event -> rollLabel.setText(SimpleFunctions.rollTheDice(d)));
                    break;
                }
                case "TIME":{
                    but.setText(but.getText()+((d == 1)?(" Minute"):(" Minutes")));
                    but.setFont(Font.font("System", FontWeight.BOLD, 25));
                    but.setOnMousePressed(event -> startTimer(SimpleFunctions.getTimeInSeconds(d)));
                    break;
                }
                default:{
                    LOGGER.error("Action Not Applied to Button Building!");
                    break;
                }
            }
            append.getChildren().add(but);
        }
    }
}