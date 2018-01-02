/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.network.ConnectionListener;
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
//        myServer.addMessageListener(new ServerListener(),
//                                    TimeMessage.class);

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
        if(playerStore.size() < 4){
            Player newPlayer = new Player(this, NODE_GAME, bulletAppState, client.getId());       
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
            // TODO : remove from the waiting list
            int indexInWaitingList = getIndexOfWaitingList(client.getId());
            waitingList.remove(indexInWaitingList);
        }
        
    }
    
    public class ServerListener implements MessageListener<HostedConnection>{

        @Override
        public void messageReceived(HostedConnection source, Message m) {

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

}
