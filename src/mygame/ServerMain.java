/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.network.ConnectionListener;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.service.serializer.ServerSerializerRegistrationsService;
import com.jme3.scene.Node;
import com.jme3.system.JmeContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import mygame.Globals.*;

/**
 *
 * @author ThomasLeScolan
 */
public class ServerMain extends SimpleApplication implements ConnectionListener{
    private Server myServer;
    private ArrayList<Player> playerStore = new ArrayList<Player>();
    private ArrayList<Integer> waitingList = new ArrayList<Integer>();
    private Object myGlobals = new Globals();
    private Node NODE_GAME = new Node("NODE_GAME");
    private BulletAppState bulletAppState;
    private Truck truck;
    private float lowUpdateTimer = 0;
    private float hightUpdateTimer = 0;
    private float lowUpdateTimerMax = 1;
    private float hightUpdateTimerMax = 0.15f;
    
    public ServerMain(){
        
    }
    
    public static void main(String[] args) {
        ServerMain app = new ServerMain();
        Globals.initialiseSerializables();
        app.start(JmeContext.Type.Headless);
    }   
    
    @Override
    public void simpleInitApp() {
        // create and start the server :
        try {
            myServer = Network.createServer(Globals.NAME, Globals.VERSION, Globals.DEFAULT_PORT, Globals.DEFAULT_PORT);
            myServer.getServices().removeService(myServer.getServices().getService(ServerSerializerRegistrationsService.class));
            myServer.start();
        } catch (IOException ex) {
            this.destroy();
            this.stop();
        }
        
        // add connection Listener :
        myServer.addConnectionListener(this);
        
        // add message listenter : 
        myServer.addMessageListener(new ServerListener(),
                                    CarParameterMessage.class);

        // init bulletAppState :
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        
        //create the floor :
        Globals.createScene(NODE_GAME, this, bulletAppState);
        
        //create the truck :
        truck = new Truck(this, NODE_GAME, bulletAppState);        
        truck.setEnabled(true);
        stateManager.attach(truck);
    }

    @Override
    public void simpleUpdate(float tpf) {
        lowUpdateTimer += tpf;
        hightUpdateTimer += tpf;
        // broadcast message all the time :
        if (lowUpdateTimer > lowUpdateTimerMax){
            lowUpdateTimer = 0;
            // position of drum
            if(truck.isEnabled()){
//                System.out.println("send mess");
                truck.sendInfo();
            }            
        }
        if (hightUpdateTimer > hightUpdateTimerMax){
            hightUpdateTimer = 0;
        }
    }
    
    // to ensure to close the net connection cleanly :
    @Override
    public void destroy() {
        try {
            myServer.close();
        } catch (Exception ex) { }
        super.destroy();
    }
    
    @Override
    public void connectionAdded(Server server, HostedConnection client) {
        // create players :
        if(playerStore.size() < 4){
            Player newPlayer = new Player(this, NODE_GAME, bulletAppState, client.getId(), false);       
            stateManager.attach(newPlayer);
            newPlayer.setEnabled(true);
            playerStore.add(newPlayer);
        }else {
            waitingList.add(client.getId()); 
        }        
    }

    @Override
    public void connectionRemoved(Server server, HostedConnection client) {
        int indexInPlayerStore = getIndexOfPlayer(client.getId());
        //  TODO : add a list player to remove 
        if (indexInPlayerStore >= 0){
            playerStore.get(indexInPlayerStore).setEnabled(false); 
            // TODO : add to the list of player to remove 
        }else {
            //remove player from the waiting list
            int indexInWaitingList = getIndexOfWaitingList(client.getId());
            waitingList.remove(indexInWaitingList);
        }
    }
    
    public class ServerListener implements MessageListener<HostedConnection>{

        @Override
        public void messageReceived(HostedConnection source, Message m) {
            if(m instanceof CarParameterMessage){
                final HostedConnection mySource = source;
                final CarParameterMessage carParamaterMess = (CarParameterMessage) m;
                Future result = ServerMain.this.enqueue(new Callable() {
                    @Override
                    public Object call() throws Exception {                        
                        playerStore.get(carParamaterMess.getID()).accelerate(carParamaterMess.getAcceleration());
                        playerStore.get(carParamaterMess.getID()).brake(carParamaterMess.getBrake());
                        playerStore.get(carParamaterMess.getID()).steer(carParamaterMess.getSteer());
                        CarParameterMessage newCarParameterMess = new CarParameterMessage(carParamaterMess.getAcceleration(), carParamaterMess.getSteer(), carParamaterMess.getBrake(), 
                                                                                        carParamaterMess.getStop(), carParamaterMess.getStart(), carParamaterMess.getAccelerationSound(),
                                                                                        carParamaterMess.getID());
                        myServer.broadcast(Filters.notEqualTo(mySource), newCarParameterMess);                                                                                        
                        return true;
                    }
                });
            }
        }        
    }
    
    private int getIndexOfWaitingList(int hostNum){
        for (int i = 0; i < waitingList.size(); i++){
            if (waitingList.get(i) == hostNum){
                return i;
            }
        }
        return -1;
    }
    
    private int getIndexOfPlayer(int hostNum){
        for (int i = 0; i < playerStore.size(); i++){
            if (playerStore.get(i).getHostNum() == hostNum){
                return i;
            }
        }
        return -1;
    }

    public Server getMyServer(){
        return myServer;
    } 
    
    private void setNewPlayer(){
        // remove players that have left the game :
        for(int i = playerStore.size() - 1; i>= 0; i--){
            if(!playerStore.get(i).isEnabled()){
                playerStore.remove(i);
            }
        }
        // add player from the waiting list :        
        while(waitingList.size() > 0 && playerStore.size() < 4){
            Player newPlayer = new Player(this, NODE_GAME, bulletAppState, waitingList.get(0), false);
            playerStore.add(newPlayer);
            waitingList.remove(0);
        }
    }
}
