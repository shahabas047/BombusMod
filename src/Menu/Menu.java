/*
 * Menu.java
 *
 * Created on 1.05.2005, 20:48
 *
 * Copyright (c) 2005-2008, Eugene Stahov (evgs), http://bombus-im.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * You can also redistribute and/or modify this program under the
 * terms of the Psi License, specified in the accompanied COPYING
 * file, as published by the Psi Project; either dated January 1st,
 * 2005, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package Menu;
import Client.Config;
import java.util.*;
import locale.SR;
import ui.*;

/**
 *
 * @author Evg_S
 */
public class Menu extends VirtualList
{
    Vector menuitems;
    private ImageList il;
    private boolean executeByNum;

    public Menu(String mainbar, ImageList il) {
        super();
        setMainBarItem(new MainBar(mainbar));
        menuitems=new Vector();
        this.il=il;
        executeByNum=Config.getInstance().executeByNum;
    }
    
    public VirtualElement getItemRef(int index){ 
        return (VirtualElement)menuitems.elementAt(index); 
    }
    public int getItemCount() { return menuitems.size(); }
    
    public void addItem(MenuItem mi){
        mi.pos=getItemCount();
        menuitems.addElement(mi);
    }
    
    public void addItem(String label, int index, int iconIndex){
        addItem(new MenuItem(label, index, iconIndex, il));
    }
    
    public void addItem(String label, int index){
        addItem(new MenuItem(label, index, -1, il));
    }
    
    public String touchLeftCommand(){ return SR.MS_OK; }
    
    public void touchLeftPressed(){
        eventOk();
    }
     
    protected boolean key(int keyCode, boolean key_long) {
        if (!key_long) {
            if (keyCode == Config.SOFT_LEFT) {
                eventOk();
                return true;
            }

            if (executeByNum && getItemCount() > 0) {
                switch (keyCode) {
                    case KEY_NUM0:
                        executeCommand(9);
                        return true;
                    case KEY_NUM1:
                        executeCommand(0);
                        return true;
                    case KEY_NUM2:
                        executeCommand(1);
                        return true;
                    case KEY_NUM3:
                        executeCommand(2);
                        return true;
                    case KEY_NUM4:
                        executeCommand(3);
                        return true;
                    case KEY_NUM5:
                        executeCommand(4);
                        return true;
                    case KEY_NUM6:
                        executeCommand(5);
                        return true;
                    case KEY_NUM7:
                        executeCommand(6);
                        return true;
                    case KEY_NUM8:
                        executeCommand(7);
                        return true;
                    case KEY_NUM9:
                        executeCommand(8);
                        return true;
                }
            }
        }

        return super.key(keyCode, key_long);
    }
    
    private void executeCommand(int index) {
        moveCursorTo(index);
        eventOk();
        /*int pos=0;
        for (Enumeration mi=menuitems.elements(); mi.hasMoreElements(); ) {
            MenuItem mit=(MenuItem)mi.nextElement();
            if (mit.index==index) {
                moveCursorTo(pos);
                eventOk();
            }
            pos++;  
        }*/
    }
    
}
