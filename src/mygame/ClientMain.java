/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.scene.Node;
import java.io.IOException;

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
    //app state : 
    private Player TestingPlayer; //TODO : array list of player
    public ClientMain(){
        
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
//        myClient.addMessageListener(new ClientListener(),
//                                    TimeMessage.class);

        //node containing all the other new node on the game :
        rootNode.attachChild(NODE_GAME);  
        
        // init bulletAppState :
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        
        //create the floor :
        myGlobals.createScene(NODE_GAME, this, bulletAppState);
        
        // create a player for testing :
        TestingPlayer = new Player(this, NODE_GAME, bulletAppState);
        stateManager.attach(TestingPlayer);
        TestingPlayer.setEnabled(true);
        
        // set up key for controlling the car :
        setupKeys();
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
            
        }
        
    }
    
    private void setupKeys() {
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(actionListener, "Lefts");
        inputManager.addListener(actionListener, "Rights");
        inputManager.addListener(actionListener, "Ups");
        inputManager.addListener(actionListener, "Downs");
//        inputManager.addListener(this, "Space");
        inputManager.addListener(actionListener, "Reset");
    }
    
    private final ActionListener actionListener = new ActionListener() {
        private boolean stop = false;
        private boolean goBack = false;
        
        @Override
        public void onAction(String binding, boolean keyPressed, float tpf) {
            if (binding.equals("Lefts")) {
                if (keyPressed) {
                    steeringValue += .15f;
                } else {
                    steeringValue += -.15f;
                }
                TestingPlayer.steer(steeringValue);
            } else if (binding.equals("Rights")) {
                if (keyPressed) {
                    steeringValue += -.15f;
                } else {
                    steeringValue += .15f;
                }
                TestingPlayer.steer(steeringValue);
            } //note that our fancy car actually goes backwards..
            else if (binding.equals("Ups")) {
                if (keyPressed) {
                    accelerationValue = - 800;
                    if (TestingPlayer.getCurrentVehicleSpeedKmHour() <= 0.2f && TestingPlayer.getCurrentVehicleSpeedKmHour() >= -0.2f){
                        TestingPlayer.playStartSoundNode();
                    }else{
                        TestingPlayer.playAccelerationSoundNode();  
                    }                    
                    TestingPlayer.stopStopSoundNode();
                } else {
                    accelerationValue = 0;
                    TestingPlayer.stopAccelerationSoundNode();
                    TestingPlayer.stopStartSoundNode();                    
                    TestingPlayer.PlayStopSoundNode();
                }
            }
            if (binding.equals("Downs")) {
                if (keyPressed) {
                    stop = true;
                    if (TestingPlayer.getCurrentVehicleSpeedKmHour() <= 0.2f && TestingPlayer.getCurrentVehicleSpeedKmHour() >= -0.2f){
                        goBack = true;
                        accelerationValue = 300;
                        stop = false;
                    }
                } else {
                    accelerationValue = 0;
                    TestingPlayer.accelerate(accelerationValue);
                    stop = false;
                    goBack = false;
                }
            } else if (binding.equals("Reset")) {
                if (keyPressed) {
                    System.out.println("Reset");
                    TestingPlayer.setPhysicsLocation(Vector3f.ZERO);
                    TestingPlayer.setPhysicsRotation(new Matrix3f());
                    TestingPlayer.setLinearVelocity(Vector3f.ZERO);
                    TestingPlayer.setAngularVelocity(Vector3f.ZERO);
                    TestingPlayer.resetSuspension();
                } else {
                }
            }
            
            if (keyPressed){
                if (goBack){                        
                    TestingPlayer.brake(0f);
                    TestingPlayer.accelerate(accelerationValue);
                } else {
                    if (stop){
                        TestingPlayer.brake(30f); 
                        accelerationValue = 0;
                        TestingPlayer.accelerate(accelerationValue);
                    }else{
                        TestingPlayer.brake(0f);
                        TestingPlayer.accelerate(accelerationValue);
                    }
                }
                
                
            }else {
                TestingPlayer.brake(5f);
            }
        } 
    };
}
