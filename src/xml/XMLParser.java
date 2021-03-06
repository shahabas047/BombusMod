/*
 * XMLParser.java
 *
 * Created on 22.03.2008, 1:02
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

package xml;

import java.io.ByteArrayOutputStream;
import java.util.*;

public class XMLParser {
    private final static int MAX_BIN_DATASIZE=64*1024; //64 KB - experimental
        
    private final static int PLAIN_TEXT=0;
    private final static int TAGNAME=1;
    private final static int ENDTAGNAME=2;
    private final static int ATRNAME=3;
    private final static int ATRVALQS=4;
    private final static int ATRVALQD=5;
    private final static int CDATA=6;
    private final static int BASE64=7;
    private final static int BASE64_INIT=8;

    private int state;
    
    private XMLEventListener eventListener;
    
    private StringBuffer sbuf;
    
    private StringBuffer tagName;
    private Vector attr;
    private String atrName;
    
    ByteArrayOutputStream baos;
    int ibuf;
    int padding;
    /** Creates a new instance of XMLParser */
    
    public XMLParser(XMLEventListener eventListener) {
        this.eventListener=eventListener;
        state=PLAIN_TEXT;
        sbuf=new StringBuffer();
        tagName=new StringBuffer();
    }
 
    public void parse(byte indata[], int size) throws XMLException{
        int dptr=0;
        while (size>0) {
            size--;
            char c=(char)(indata[dptr++] &0xff);
            switch (state) {
                case PLAIN_TEXT: {
                    //parsing plain text
                    if (c=='<') {
                        state=TAGNAME;
                        
                        if (sbuf.length()>0) 
                            eventListener.plainTextEncountered( parsePlainText(sbuf) ); 
                        
                        sbuf=new StringBuffer();
                        tagName=new StringBuffer();
                        attr=null;
                        attr=new Vector();
                        
                        continue;
                    }
                    sbuf.append(c); continue;
                }

        case ATRNAME:
            {
                if (c=='?') continue;
                if (c==' ') continue;
                if (c=='=') continue;
                if (c=='\'') { state=ATRVALQS; atrName=parsePlainText(sbuf); sbuf=new StringBuffer(); continue; }
                if (c=='\"') { state=ATRVALQD; atrName=parsePlainText(sbuf); sbuf=new StringBuffer(); continue; }

                if (c!='>' && c!='/') { 
                    sbuf.append(c);
                    continue;
                } else {
                    state=TAGNAME;
                    sbuf=new StringBuffer();
                }
            }

        case TAGNAME:
            {
                if (c=='?') continue;
                if (c=='/') { 
                    state=ENDTAGNAME; 
                    sbuf=new StringBuffer();
                    if (tagName.length()>0) {
                        String tn= parsePlainText(tagName);
                        eventListener.tagStart(localName(tn, TAGNAME), attr); 
                        sbuf.append(tn);
                    }
                    continue; 
                }
                if (c==' ') { state=ATRNAME; continue; }
                if (c=='>') { 
                    state=PLAIN_TEXT; 
                    if (eventListener.tagStart(localName(parsePlainText(tagName), TAGNAME), attr))
                        state=BASE64_INIT; 
                    continue; 
                }
                tagName.append(c);

                if (c=='[') {
                    if (tagName.toString().equals("![CDATA[")) 
                        state=CDATA;
                    continue;
                }

                continue;
            }

        case CDATA:
            {
                sbuf.append(c);
                if (c=='>') {
                    int e3=sbuf.length()-3;
                    if (e3 < 0) continue;
                    if (sbuf.charAt(e3) != ']') continue;
                    if (sbuf.charAt(e3+1) != ']') continue;
                    //if (sbuf[e3] != '>') continue;
                    sbuf.setLength(e3);
                    state=PLAIN_TEXT;
                    continue;
                }
                continue;
            }
        case ENDTAGNAME:
            {
                if (c==' ') continue;
                if (c=='>') {
                    state=PLAIN_TEXT;
                    eventListener.tagEnd(localName(sbuf.toString(), TAGNAME));
                    sbuf=new StringBuffer();
                    continue;
                }
                sbuf.append(c);
                continue;
            }
            
        case ATRVALQS: 
            {
                if (c=='\'') { 
                    state=ATRNAME; 
                    attr.addElement(localName(atrName, ATRNAME));
                    attr.addElement(parsePlainText(sbuf));
                    sbuf=new StringBuffer(); 
                    continue; 
                }
                sbuf.append(c);
                continue;
            }
        case ATRVALQD: 
            {
                if (c=='\"') { 
                    state=ATRNAME; 
                    attr.addElement(localName(atrName, ATRNAME));
                    attr.addElement(parsePlainText(sbuf));
                    sbuf=new StringBuffer(); 
                    continue; 
                }
                sbuf.append(c);
                continue;
            }
        
        case BASE64_INIT: 
            {
                baos=new ByteArrayOutputStream(MAX_BIN_DATASIZE);
                ibuf=1;
                padding=0;
                state=BASE64;
            }
        case BASE64: 
            {
                int base64=-1;
                if (c > 'A'-1  &&  c < 'Z'+1) base64 =  c - 'A';
                else if (c > 'a'-1  &&  c < 'z'+1) base64 =  c +26-'a';
                else if (c > '0'-1  &&  c < '9'+1) base64 =  c +52-'0';
                else if (c == '+') base64=62;
                else if (c == '/') base64=63;
                else if (c == '=') {base64=0; padding++;}
                
                else if (c == '<') {
                    try { 
                        baos.close();
                        
                        if (baos.size()<MAX_BIN_DATASIZE)
                            eventListener.binValueEncountered( baos.toByteArray() );
                        else
                            eventListener.binValueEncountered( new byte[1] );
                    } catch (Exception ex) { 
                    }
                    
                    baos=null;
                    sbuf=new StringBuffer();
                    tagName=new StringBuffer();
                    state=TAGNAME;
                    continue;
                }
                
                if (base64>=0) ibuf=(ibuf<<6)+base64;
                if (baos.size()<MAX_BIN_DATASIZE) {
                    if (ibuf>=0x01000000){
                        baos.write((ibuf>>16) &0xff);
                        if (padding<2) baos.write((ibuf>>8) &0xff);
                        if (padding==0) baos.write(ibuf &0xff);
                        //len+=3;
                        ibuf=1;
                    }
                    
                }
                
                continue;
            }
        }
    }
        
    };
    
    private String parsePlainText(StringBuffer sb) throws XMLException {
        //1. output text length will be not greather than source
        //2. sb may be destroyed - all calls to parsePlainText succeeds flushing of sb
        int ipos=0;
        int opos=0;
        while (ipos<sb.length()) {
            char c=sb.charAt(ipos++);
            if (c=='&') { 
                StringBuffer xmlChar=new StringBuffer(6);
                while (true) {
                    c=sb.charAt(ipos++);
                    if (c==';') break;
                    xmlChar.append(c);
                }
                String s=xmlChar.toString();

                if (s.equals("amp")) c='&'; else
                if (s.equals("apos")) c='\''; else
                if (s.equals("quot")) c='\"'; else
                if (s.equals("gt")) c='>'; else
                if (s.equals("lt")) c='<'; else
                if (xmlChar.charAt(0)=='#') {
                    xmlChar.deleteCharAt(0);
                    c=(char)Integer.parseInt(xmlChar.toString());
                }
                sb.setCharAt(opos++, c); 
                continue;
            }
            if (c<0x80) { 
                sb.setCharAt(opos++, c); 
                continue; 
            }

            if (c<0xc0) throw new XMLException("Bad UTF-8 Encoding encountered");

            char c2=sb.charAt(ipos++);
            if (c2<0x80) throw new XMLException("Bad UTF-8 Encoding encountered");

            if (c<0xe0) {
                sb.setCharAt(opos++, (char)(((c & 0x1f)<<6) | (c2 &0x3f)) );
                continue;
            }

            char c3=sb.charAt(ipos++);
            if (c3<0x80) throw new XMLException("Bad UTF-8 Encoding encountered");

            if (c<0xf0) {
                sb.setCharAt(opos++, (char)(((c & 0x0f)<<12) | ((c2 &0x3f) <<6) | (c3 &0x3f)) );
                continue;
            }
            
            char c4=sb.charAt(ipos++);
            if (c4<0x80) throw new XMLException("Bad UTF-8 Encoding encountered");
        
            //return ((chr & 0x07)<<18) | ((chr2 &0x3f) <<12) |((chr3 &0x3f) <<6) | (chr4 &0x3f);
            sb.setCharAt(opos++, '?'); // java char type contains only 16-bit symbols
            continue;
            
        }        

        sb.setLength(opos);
        return sb.toString();
    }

    public void pushOutPlainText() throws XMLException {
        if (state==PLAIN_TEXT) {
            if (sbuf.length()>0)
                eventListener.plainTextEncountered( parsePlainText(sbuf) );
            
            sbuf=new StringBuffer();
        }
    }

    public static String extractAttribute(String attributeName, Vector attributes) {
      if (attributes==null) return null;
      int index=0;
      int j=attributes.size();
      while (index<j) {
          if ( ((String)attributes.elementAt(index)).equals(attributeName) )
              return (String)attributes.elementAt(index+1);
          
          index+=2;
      }
      
      return null;
    }
    
    public static String localName(String entity, int type) {
        int delim = entity.indexOf(':');
        if (delim < 0) 
            return entity;
        String localName = entity;
        switch (type) {
            case ATRNAME:
                localName = entity.substring(0, delim); 
                break;
            case TAGNAME:                               
                localName = entity.substring(delim + 1);
                break;                
        }
        return localName;        
    }
}
