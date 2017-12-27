/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.network.serializing.Serializer;

/**
 *
 * @author ThomasLeScolan
 */
public class Globals {
    // variable for setting the server and the clients :
    public static final String NAME = "Lab3";
    public static final String DEFAULT_SERVER = "Localhost"; //"130.240.157.44";
    public static final int VERSION = 1;
    public static final int DEFAULT_PORT = 6143;
    
    // register all message types there are
    public static void initialiseSerializables() {
//        Serializer.registerClass(TimeMessage.class);
    }
}
