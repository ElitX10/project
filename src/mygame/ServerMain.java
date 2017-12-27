/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.service.serializer.ServerSerializerRegistrationsService;
import com.jme3.system.JmeContext;
import java.io.IOException;

/**
 *
 * @author ThomasLeScolan
 */
public class ServerMain extends SimpleApplication implements ConnectionListener{
     private Server myServer;
    
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

    }

    @Override
    public void connectionRemoved(Server server, HostedConnection client) {

    }
    
    public class ServerListener implements MessageListener<HostedConnection>{

        @Override
        public void messageReceived(HostedConnection source, Message m) {

        }
        
    }

}
