/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
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
import com.jme3.scene.Node;
import com.jme3.util.SkyFactory;
import java.io.IOException;
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
    //app state : 
    private ArrayList<Player> playerStore = new ArrayList<Player>();
    private Truck truck; 
    private ClientRace myRace;
    
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
                                    timeMessage.class);

        //node containing all the other new node on the game :
        rootNode.attachChild(NODE_GAME);  
        
        // init bulletAppState :
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        
        //create the floor :
        Globals.createScene(NODE_GAME, this, bulletAppState);
        
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
    }

    @Override
    public void simpleUpdate(float tpf) {
        
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
                                if(Math.abs(playerLocation.x - X[i]) > 1.5f || Math.abs(playerLocation.y - Y[i]) > 1.5f || Math.abs(playerLocation.z - Z[i]) > 1.5f){
                                    playerStore.get(i).setPosition(X[i], Y[i], Z[i]);
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
            }else if (m instanceof timeMessage){
                final timeMessage timeMess = (timeMessage) m;
                Future result = ClientMain.this.enqueue(new Callable() {
                    @Override
                    public Object call() throws Exception {
                                                
                        return true;
                    }
                });
            }
        }
        
    }
    
    private void initHUD(){
        waitMessage = new BitmapText(guiFont);
        waitMessage.setName("waitMessage");
        waitMessage.setSize(guiFont.getCharSet().getRenderedSize());
        waitMessage.setColor(ColorRGBA.Blue);
        waitMessage.setText("Player waiting");
        waitMessage.setLocalTranslation(settings.getWidth() / 2, settings.getHeight() / 2, 0);
        guiNode.attachChild(waitMessage);
//        guiNode.detachChildNamed(waitMessage.getName());
    }
    
    private void setupKeys(boolean turnOn) {
        if(turnOn){
            // able key input
            if(!inputManager.hasMapping("Lefts")){
                inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_H));
                inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_K));
                inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_U));
                inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_J));
                inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
            }    
                inputManager.addListener(actionListener, "Lefts");
                inputManager.addListener(actionListener, "Rights");
                inputManager.addListener(actionListener, "Ups");
                inputManager.addListener(actionListener, "Downs");
                inputManager.addListener(actionListener, "Reset");  
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
