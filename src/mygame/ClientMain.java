/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.util.SkyFactory;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import mygame.Globals.*;

/**
 *
 * @author ThomasLeScolan
 */
public class ClientMain extends SimpleApplication implements ClientStateListener{

    private Client myClient;
    private Globals myGlobals = new Globals();
    private Node NODE_GAME = new Node("NODE_GAME");
    private BulletAppState bulletAppState;
    private float steeringValue = 0;
    private float accelerationValue = 0;
    private float brakingValue =0;
    private int controlledPlayerID = -1; // TODO : set this depending of the information of the server !
    private BitmapText waitMessage;
    DecimalFormat df = new DecimalFormat("0.0 s");
    private float myTime = 0;
    private BitmapText timeHUD;
    private BitmapText results; 
    //app state : 
    private ArrayList<Player> playerStore = new ArrayList<Player>();
    private Truck truck; 
    private ClientRace myRace;
    
    Node checkpointNode;
    Geometry checkpoint1;
    Geometry checkpoint2;
    private int lap = 0;
    private int nextCheckpoint = 1;
    private int ResetTime = 0;
    private boolean finished = false;
    
    
    public ClientMain(){
        myRace = new ClientRace(this, Globals.RACETIME);
        stateManager.attach(myRace);
    }
    
    public static void main(String[] args) {
        ClientMain app = new ClientMain();
        Globals.initialiseSerializables();
        app.start(/*JmeContext.Type.Display*/);
    }
    
    public Globals getMyGlobals(){
        return myGlobals;
    }
    
    public Client getMyClient(){
        return myClient;
    }
    
    public Node getGameNode(){
        return NODE_GAME;
    }
    
    @Override
    public void simpleInitApp() {
        // create and start the client :
        try {
            myClient = Network.connectToServer(Globals.NAME, Globals.VERSION, Globals.DEFAULT_SERVER, Globals.DEFAULT_PORT);
            myClient.start();
        } catch (IOException ex) {
            this.destroy();
            this.stop();
        }
        
        // add client listener :
        myClient.addClientStateListener(this);
        
        // add message listenter :
        myClient.addMessageListener(new ClientListener(),
                                    RandomEventMessage.class,
                                    DrumPositionMessage.class,
                                    CarPositionMessage.class,
                                    CarParameterMessage.class,
                                    TimeMessage.class,
                                    ScoreMessage.class,
                                    ResultMessage.class);

        //node containing all the other new node on the game :
        rootNode.attachChild(NODE_GAME);  
        
        // init bulletAppState :
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        
        //create the floor :
        Globals.createScene(NODE_GAME, this, bulletAppState);
        
        //Checkpoints
        Globals.createCheckpoint(NODE_GAME, this, bulletAppState);
        checkpointNode = (Node) NODE_GAME.getChild("checkpoint");
        checkpoint1 = (Geometry) checkpointNode.getChild("Checkpoint1");
        checkpoint2 = (Geometry) checkpointNode.getChild("Checkpoint2");
        checkpointNode.detachChildNamed("Checkpoint1");
        
//        // create a player for testing :
//        Player TestingPlayer = new Player(this, NODE_GAME, bulletAppState, true);       
//        stateManager.attach(TestingPlayer);
//        TestingPlayer.setEnabled(true);
//        playerStore.add(TestingPlayer);
        
        //create the truck :
        truck = new Truck(this, NODE_GAME, bulletAppState);        
        truck.setEnabled(true);
        stateManager.attach(truck);
        
        // set up key for controlling the car :
//        setupKeys(true); //TODO : set up key when the race start !!
        
        // sky :
        getRootNode().attachChild(SkyFactory.createSky(getAssetManager(), "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap));
        
        // hud :
        initHUD();
        
        flyCam.setEnabled(false);
        setDisplayStatView(false);
        setDisplayFps(false);
        setPauseOnLostFocus(false);
        
        finished = false;
        
        results = new BitmapText(guiFont);
        results.setName("result");
        
        // nature sound :
        AudioNode audio_nature = new AudioNode(assetManager, "Sound/Environment/Nature.ogg", true);
        audio_nature.setPositional(false);
        audio_nature.setLooping(true);
        audio_nature.setVolume(0.5f);
        rootNode.attachChild(audio_nature);
        audio_nature.play();
    }

    @Override
    public void simpleUpdate(float tpf) {
        myTime -= tpf;
        timeHUD.setText("Time : " + df.format(myTime));
        
        if (controlledPlayerID < 0){
            
            cam.setLocation(new Vector3f(0, 4, 30));
        }else{
            if(myRace.isEnabled()){
                if(nextCheckpoint == 0){
                    if(checkpoint1.getWorldBound().intersects(playerStore.get(controlledPlayerID).getPosition())){
                        System.out.println("checked");
                        nextCheckpoint = 1;
                        lap += 1;
                        checkpointNode.attachChild(checkpoint2);
                        checkpointNode.detachChildNamed("Checkpoint1");       
                    }
                }
                else if(nextCheckpoint == 1){
                    if(checkpoint2.getWorldBound().intersects(playerStore.get(controlledPlayerID).getPosition())){
                        System.out.println("checked");
                        nextCheckpoint = 0;    
                        checkpointNode.attachChild(checkpoint1);
                        checkpointNode.detachChildNamed("Checkpoint2");
                    }
                }
                if(lap == 1){
                    if(!finished){
                        System.out.println("You Win!");
                        FinishMessage finishMess = new FinishMessage(myTime, controlledPlayerID);
                        myClient.send(finishMess);
                        finished = true;
                        
                        // stop the car :
                        playerStore.get(controlledPlayerID).steer(0);
                        playerStore.get(controlledPlayerID).brake(30f);
                        playerStore.get(controlledPlayerID).accelerate(0);
                        CarParameterMessage carParameter = new CarParameterMessage(0, 0, 30f, false, false, false, controlledPlayerID);
                        myClient.send(carParameter);
                        setupKeys(false);
                        playerStore.get(controlledPlayerID).stopAccelerationSoundNode();
                        playerStore.get(controlledPlayerID).stopStartSoundNode();
                        playerStore.get(controlledPlayerID).stopStopSoundNode();
                    }
                }
    //            if(ResetTime == 15){
    //                ResetLoc = player.getPhysicsLocation();
    //                ResetTime = 0;
    //            }else{
    //                ResetTime += 1;
    //            }
            }
        }

    }
    
    // to ensure to close the net connection cleanly :
    @Override
    public void destroy() {
        try {
            myClient.close();
        } catch (Exception ex) { }
        super.destroy();
    }
    
    @Override
    public void clientConnected(Client client) {

    }

    @Override
    public void clientDisconnected(Client client, DisconnectInfo info) {

    }
    
    public class ClientListener implements MessageListener<Client> {

        @Override
        public void messageReceived(Client source, Message m) {
            if(m instanceof RandomEventMessage){
                Future result = ClientMain.this.enqueue(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        truck.triggerEvent();
                        return true;
                    }
                }); 
            }else if(m instanceof DrumPositionMessage){
                final DrumPositionMessage drumPosMess = (DrumPositionMessage) m;
                Future result = ClientMain.this.enqueue(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        truck.sendInfo();
                        float[] X = drumPosMess.getX();
                        float[] Y = drumPosMess.getY();
                        float[] Z = drumPosMess.getZ();
                        float[][] rot = drumPosMess.getRotation();
                        for(int i = 0; i < 3; i++){
                            if(Math.abs(truck.getX()[i] - X[i]) > 1.5f || Math.abs(truck.getY()[i] - Y[i]) > 1.5f || Math.abs(truck.getZ()[i] - Z[i]) > 1.5f){
                                truck.moveTo(X[i], Y[i], Z[i], i);
                                truck.setRotation(rot, i);
                            }
                        }                        
                        return true;
                    }
                });
            }else if(m instanceof CarParameterMessage){
                final CarParameterMessage carParamaterMess = (CarParameterMessage) m;
                Future result = ClientMain.this.enqueue(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        if (!playerStore.isEmpty() && myRace.getTime() < 1){
                            playerStore.get(carParamaterMess.getID()).accelerate(carParamaterMess.getAcceleration());
                            playerStore.get(carParamaterMess.getID()).brake(carParamaterMess.getBrake());
                            playerStore.get(carParamaterMess.getID()).steer(carParamaterMess.getSteer());
                            if(carParamaterMess.getStart()){
                                playerStore.get(carParamaterMess.getID()).playStartSoundNode();
                            }else{
                                playerStore.get(carParamaterMess.getID()).stopStartSoundNode();
                            } 
                            if(carParamaterMess.getStop()){
                                playerStore.get(carParamaterMess.getID()).playStopSoundNode();
                            }else{
                                playerStore.get(carParamaterMess.getID()).stopStopSoundNode();
                            } 
                            if(carParamaterMess.getAccelerationSound()){
                                playerStore.get(carParamaterMess.getID()).playAccelerationSoundNode();
                            }else{
                                playerStore.get(carParamaterMess.getID()).stopAccelerationSoundNode();
                            } 
                        }
                        return true;
                    }
                });
            }else if(m instanceof CarPositionMessage){
                final CarPositionMessage carPositionMess = (CarPositionMessage) m;
                final int myHost = myClient.getId();
                Future result = ClientMain.this.enqueue(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        float[] X = carPositionMess.getX();
                        float[] Y = carPositionMess.getY();
                        float[] Z = carPositionMess.getZ();
                        float[][] rot = carPositionMess.getRotation();
                        
                        if (playerStore.isEmpty()){
                            for (int i = 0; i < X.length; i++){
                                Player newPlayer; 
                                if (myHost == carPositionMess.getHost()[i]){
                                    newPlayer = new Player(ClientMain.this, NODE_GAME, bulletAppState, true);
                                    myRace.setEnabled(true);
                                    guiNode.detachChildNamed(waitMessage.getName());
                                    guiNode.detachChildNamed(results.getName());
                                    setupKeys(true);
                                    controlledPlayerID = i;
                                }else{
                                    newPlayer = new Player(ClientMain.this, NODE_GAME, bulletAppState, false);
                                }
                                stateManager.attach(newPlayer);
                                newPlayer.setEnabled(true);
                                playerStore.add(newPlayer);
                            }
                        }else {
                            for (int i = 0; i < X.length ; i++){
                                Vector3f playerLocation = playerStore.get(i).getPosition();
                                float a = 0.5f;
                                if(Math.abs(playerLocation.x - X[i]) > 0.5f || Math.abs(playerLocation.y - Y[i]) > 0.5f || Math.abs(playerLocation.z - Z[i]) > 0.5f){
                                    if (!playerStore.get(i).getState()){
                                        playerStore.get(i).setPosition(X[i], Y[i], Z[i]);
                                        playerStore.get(i).initPlayer();
                                    }else{
                                        playerStore.get(i).setPosition(X[i] + a * (playerLocation.x - X[i]), Y[i] + a * (playerLocation.y - Y[i]), Z[i] + a * (playerLocation.z - Z[i]));
                                    }                                    
                                    playerStore.get(i).setRotation(new Matrix3f(rot[i][0], rot[i][1], rot[i][2],
                                                                                rot[i][3], rot[i][4], rot[i][5], 
                                                                                rot[i][6], rot[i][7], rot[i][8]));
                                }
                            }
                        }
//                        for(int i = 0; i < 3; i++){
//                            if(Math.abs(truck.getX()[i] - X[i]) > 1.5f || Math.abs(truck.getY()[i] - Y[i]) > 1.5f || Math.abs(truck.getZ()[i] - Z[i]) > 1.5f){
//                                truck.moveTo(X[i], Y[i], Z[i], i);
//                                truck.setRotation(rot, i);
//                            }
//                        }                        
                        return true;
                    }
                });
            }else if (m instanceof TimeMessage){
                final TimeMessage timeMess = (TimeMessage) m;
                Future result = ClientMain.this.enqueue(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        setTime(timeMess.getTime());
                        return true;
                    }
                });
            }else if (m instanceof ScoreMessage){
                final ScoreMessage scoreMess = (ScoreMessage) m;                
                Future result = ClientMain.this.enqueue(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        myRace.setEnabled(false);
                        guiNode.attachChild(waitMessage);
                        return true;
                    }
                });
            }else if (m instanceof ResultMessage){
            final ResultMessage raceResults = (ResultMessage) m;
            Future result = ClientMain.this.enqueue(new Callable() {
               @Override
               public Object call() throws Exception{
                    results.setSize(guiFont.getCharSet().getRenderedSize() * 2);
                    results.setColor(ColorRGBA.Blue);
                    results.setText("You got position " + raceResults.getResults(controlledPlayerID));
                    results.setLocalTranslation(settings.getWidth() / 2 - results.getLineWidth() / 2, settings.getHeight() / 2 - 2 * results.getLineHeight(), 0);
                    guiNode.attachChild(results);
                   return true;
               }
            });
        }
        }
        private void setTime(float newTime){
            myTime = newTime;
        }
    }
    
    private void initHUD(){
        waitMessage = new BitmapText(guiFont);
        waitMessage.setName("waitMessage");
        waitMessage.setSize(guiFont.getCharSet().getRenderedSize() * 3);
        waitMessage.setColor(ColorRGBA.Blue);
        waitMessage.setText("Player waiting");
        waitMessage.setLocalTranslation(settings.getWidth() / 2 - waitMessage.getLineWidth() / 2, settings.getHeight() / 2 , 0);
        guiNode.attachChild(waitMessage);
//        guiNode.detachChildNamed(waitMessage.getName());

        timeHUD = new BitmapText(guiFont);
        timeHUD.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        timeHUD.setColor(ColorRGBA.Blue);
        timeHUD.setText("Time : " + df.format(myTime));
        timeHUD.setLocalTranslation(10, settings.getHeight(), 0);
        guiNode.attachChild(timeHUD);
    }
    
    private void setupKeys(boolean turnOn) {
        if(turnOn){
            // able key input
            if(!inputManager.hasMapping("Lefts")){
                inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_H));
                inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_K));
                inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_U));
                inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_J));
                //inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
            }    
                inputManager.addListener(actionListener, "Lefts");
                inputManager.addListener(actionListener, "Rights");
                inputManager.addListener(actionListener, "Ups");
                inputManager.addListener(actionListener, "Downs");
                //inputManager.addListener(actionListener, "Reset");  
        }else{
            // unable key input :
            inputManager.removeListener(actionListener);             
        }
        
    }
    
    private final ActionListener actionListener = new ActionListener() {
        private boolean stop = false;
        private boolean goBack = false;
        private boolean stopSound = false;
        private boolean startSound = false;
        private boolean accelerationSound = false;
        
        // control the car :
        @Override
        public void onAction(String binding, boolean keyPressed, float tpf) {
            if (binding.equals("Lefts")) {
                if (keyPressed) {
                    steeringValue = .15f;
                } else {
                    steeringValue = 0;
                }
                playerStore.get(controlledPlayerID).steer(steeringValue);
            } else if (binding.equals("Rights")) {
                if (keyPressed) {
                    steeringValue = -.15f;
                } else {
                    steeringValue = 0;
                }
                playerStore.get(controlledPlayerID).steer(steeringValue);
            } //note that our fancy car actually goes backwards..
            else if (binding.equals("Ups")) {
                if (keyPressed) {
                    accelerationValue = - 800;
                    if (playerStore.get(controlledPlayerID).getCurrentVehicleSpeedKmHour() <= 0.2f && playerStore.get(controlledPlayerID).getCurrentVehicleSpeedKmHour() >= -0.2f){
                        playerStore.get(controlledPlayerID).playStartSoundNode();
                        startSound = true;
                    }else{
                        playerStore.get(controlledPlayerID).playAccelerationSoundNode();                        
                        accelerationSound = true;
                    }                    
                    playerStore.get(controlledPlayerID).stopStopSoundNode();
                    stopSound = false;
                } else {
                    setupKeys(false);
                    setupKeys(true);
                    accelerationValue = 0;
                    playerStore.get(controlledPlayerID).stopAccelerationSoundNode();
                    playerStore.get(controlledPlayerID).stopStartSoundNode();                    
                    playerStore.get(controlledPlayerID).playStopSoundNode();
                    stopSound = true;
                    startSound = false;
                    accelerationSound = false;
                }
            }
            if (binding.equals("Downs")) {
                if (keyPressed) {
                    stop = true;
                    if (playerStore.get(controlledPlayerID).getCurrentVehicleSpeedKmHour() <= 0.2f && playerStore.get(controlledPlayerID).getCurrentVehicleSpeedKmHour() >= -0.2f){
                        goBack = true;
                        accelerationValue = 300;
                        stop = false;
                    }
                } else {
                    accelerationValue = 0;
                    playerStore.get(controlledPlayerID).accelerate(accelerationValue);
                    stop = false;
                    goBack = false;
                }
            } else if (binding.equals("Reset")) {
                if (keyPressed) {
                    System.out.println("Reset");
                    playerStore.get(controlledPlayerID).setPhysicsLocation(Vector3f.ZERO);
                    playerStore.get(controlledPlayerID).setPhysicsRotation(new Matrix3f());
                    playerStore.get(controlledPlayerID).setLinearVelocity(Vector3f.ZERO);
                    playerStore.get(controlledPlayerID).setAngularVelocity(Vector3f.ZERO);
                    playerStore.get(controlledPlayerID).resetSuspension();
                }
            }            
            if (keyPressed){
                if (goBack){   
                    brakingValue = 0f;
                    playerStore.get(controlledPlayerID).brake(brakingValue);
                    playerStore.get(controlledPlayerID).accelerate(accelerationValue);
                } else {
                    if (stop){
                        brakingValue = 30f;
                        playerStore.get(controlledPlayerID).brake(brakingValue); 
                        accelerationValue = 0;
                        playerStore.get(controlledPlayerID).accelerate(accelerationValue);
                    }else{
                        brakingValue = 0f;
                        playerStore.get(controlledPlayerID).brake(brakingValue);
                        playerStore.get(controlledPlayerID).accelerate(accelerationValue);
                    }
                }    
            }else {
                brakingValue = 6f;
                playerStore.get(controlledPlayerID).brake(brakingValue);
                playerStore.get(controlledPlayerID).accelerate(accelerationValue);
            }
            // Send message to the server :
            CarParameterMessage carParameter = new CarParameterMessage(accelerationValue, steeringValue, brakingValue, stopSound, startSound, accelerationSound, controlledPlayerID);
            myClient.send(carParameter);
        } 
    };
    
    public void resetPlayerStore(){
        for (int i = 0; i < playerStore.size(); i++){
            playerStore.get(i).setEnabled(false);
        }
        setupKeys(false);
        playerStore.clear();
    }
}

//-------------------------------------------------GAME---------------------------------------------------------------------------------------
class ClientRace extends BaseAppState{
    private ClientMain myApp;
    private int maxTime = 0;
    private float currentTime;
    private final float saveTime;
    
    public ClientRace(ClientMain app, int raceTime){
        myApp = app;
        currentTime = raceTime;
        saveTime = raceTime;
    }
    
    @Override
    protected void initialize(Application app) {
        
    }

    @Override
    public void update(float tpf) {
        currentTime -= tpf;
        if(currentTime < maxTime){
            currentTime = saveTime;
            this.setEnabled(false);
        }
    }
    
    @Override
    protected void cleanup(Application app) {
        
    }

    @Override
    protected void onEnable() {
//        myApp.setNewPlayer();
//        myApp.initPlayer();
    }

    @Override
    protected void onDisable() {        
        myApp.resetPlayerStore();
    }
    
    public float getTime(){
        return currentTime;
    }
    
    public void setTime(float time){
        currentTime = time;
    }
}
