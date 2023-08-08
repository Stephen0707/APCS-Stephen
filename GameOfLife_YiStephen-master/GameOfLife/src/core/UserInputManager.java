//---------------------------------------------------------------------------------
//DO NOT MODIFY!!!
//---------------------------------------------------------------------------------

package core;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class UserInputManager extends MouseAdapter
{
    // Represents ObjectLandPanel
    private JPanel panel;

    // Keyboard management
    private final ReentrantLock keypressLock;	// Protects pressedKey
    private String pressedKey;
    private KeyEventDispatcher dispatcher;
    private HashMap<Integer, String> keyCodeToKeyString;

    // Calling getPressedKey two times in a row can cause issues for
    // unsuspecting students.  If the first call returns non-null,
    // they assume the second one will as well, but it probably won't.
    // These fields track if a student is doing this, and fails early
    // with an exception to warn them.
    private String multiKeyCallErrorMessage;
    private int numKeyCalls;

    // Mouse management
    private final ReentrantLock mouseLock;
    private MouseEvent downMouseEventCurrent;       // State right now
    private boolean wasDownMouseEventCurrentSeen;   // Did user see our state yet?
    private MouseEvent downMouseEventPrevious;      // Prior state unseen by user

    public UserInputManager(JPanel panelP)
    {
        panel = panelP;

        // Keypresses

        keypressLock = new ReentrantLock();
        initKeyCodeToKeyStringMap();
        dispatcher = new KeyEventDispatcher() 
        {
            public boolean dispatchKeyEvent (KeyEvent event) 
            {
                KeyStroke stroke = javax.swing.KeyStroke.getKeyStrokeForEvent(event);
                int keyCode = stroke.getKeyCode();

                if (event.getID() == KeyEvent.KEY_PRESSED)
                {
                    keypressLock.lock();
                    try 
                    {
                        pressedKey = keyCodeToKeyString.get(keyCode);
                    }
                    finally 
                    {
                        keypressLock.unlock();
                    }
                }

                // Note: Intentionally do not process KEY_RELEASED.  Otherwise,
                // if the student's game is pausing while the player presses and
                // releases a key, then the keypress would never been noticed.
                // Instead, pressedKey will be reset to NULL upon a call to getPressedKey
                // (so it won't appear like a key is pressed double).  This leads to
                // model where a keypress is "sticky"--a key appears down until the game notices it,
                // after which it appears released.  Keypresses are not queued--last keypress
                // wins if the game is not checking keypresses often enough.

                return false;
            }
        };

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addKeyEventDispatcher(dispatcher);

        // Mouse clicks
        mouseLock = new ReentrantLock();
        downMouseEventCurrent = null;
        wasDownMouseEventCurrentSeen = false;
        downMouseEventPrevious = null;
        panel.addMouseListener(this);
    }

    public void close()
    {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
        panel.removeMouseListener(this);
        dispatcher = null;
        pressedKey = null;
    }

    // If the project decides that calling getPressedKey two times
    // in a row is dangerous, this is used to warn students if they try
	public void disallowMultipleKeyCalls(String errorMessage)
	{
		numKeyCalls = 0;
		multiKeyCallErrorMessage = errorMessage;
	}

	// This resets back to the default behavior of allowing getPressedKey
	// to be called anytime
	public void allowMultipleKeyCalls()
	{
		multiKeyCallErrorMessage = null;
	}

	public String getPressedKey()
    {
    	if (multiKeyCallErrorMessage != null && numKeyCalls >= 1)
    	{
    		throw new UnsupportedOperationException(multiKeyCallErrorMessage);
    	}
    	numKeyCalls++;
    	
        String ret = null;
        keypressLock.lock();
        try 
        {
            ret = pressedKey;

            // Setting to null prevents a mis-perception of a double-press
            pressedKey = null; 
        }
        finally 
        {
            keypressLock.unlock();
        }

        return ret;
    }

    private void initKeyCodeToKeyStringMap()
    {
        keyCodeToKeyString = new HashMap<Integer, String>();
        keyCodeToKeyString.put(KeyEvent.VK_ENTER, "ENTER");
        keyCodeToKeyString.put(KeyEvent.VK_BACK_SPACE, "BACKSPACE");
        keyCodeToKeyString.put(KeyEvent.VK_TAB, "TAB");
        keyCodeToKeyString.put(KeyEvent.VK_SHIFT, "SHIFT");
        keyCodeToKeyString.put(KeyEvent.VK_CONTROL, "CONTROL");
        keyCodeToKeyString.put(KeyEvent.VK_ALT, "ALT");
        keyCodeToKeyString.put(KeyEvent.VK_ESCAPE, "ESCAPE");
        keyCodeToKeyString.put(KeyEvent.VK_SPACE, "SPACE");
        keyCodeToKeyString.put(KeyEvent.VK_LEFT, "LEFT");
        keyCodeToKeyString.put(KeyEvent.VK_UP, "UP");
        keyCodeToKeyString.put(KeyEvent.VK_RIGHT, "RIGHT");

        keyCodeToKeyString.put(KeyEvent.VK_COMMA, ",");
        keyCodeToKeyString.put(KeyEvent.VK_MINUS, "-");
        keyCodeToKeyString.put(KeyEvent.VK_PERIOD, ".");
        keyCodeToKeyString.put(KeyEvent.VK_SLASH, "/");
        keyCodeToKeyString.put(KeyEvent.VK_SEMICOLON, ";");
        keyCodeToKeyString.put(KeyEvent.VK_EQUALS, "=");
        keyCodeToKeyString.put(KeyEvent.VK_OPEN_BRACKET, "[");
        keyCodeToKeyString.put(KeyEvent.VK_BACK_SLASH, "\\");
        keyCodeToKeyString.put(KeyEvent.VK_CLOSE_BRACKET, "]");

        for (char ch = 'A'; ch <= 'Z'; ch++)
        {
            keyCodeToKeyString.put((int) ch, "" + ch);
        }

        for (char ch = '0'; ch <= '9'; ch++)
        {
            keyCodeToKeyString.put((int) ch, "" + ch);
        }

        for (int i = KeyEvent.VK_NUMPAD0; i <= KeyEvent.VK_NUMPAD9; i++)
        {
            keyCodeToKeyString.put(i, "" + (i - KeyEvent.VK_NUMPAD0));
        }
    }

    // Dispatcher calls this when button is pressed
    @Override
    public void mousePressed(MouseEvent e)
    {
        // Only expected mouse events are accepted

        if (e.getID() != MouseEvent.MOUSE_PRESSED)
        {
            return;
        }

        int button = e.getButton();
        if (button != MouseEvent.BUTTON1 && button != MouseEvent.BUTTON3)
        {
            return;
        }

        processMousePressed(e);
    }

    private void processMousePressed(MouseEvent e)
    {
        mouseLock.lock();
        try 
        {
            downMouseEventCurrent = e;
            wasDownMouseEventCurrentSeen = false;
            downMouseEventPrevious = null;
        }
        finally 
        {
            mouseLock.unlock();
        }
    }

    // Dispatcher calls this when button is released
    @Override
    public void mouseReleased(MouseEvent e)
    {
        // Only expected mouse events are accepted

        if (e.getID() != MouseEvent.MOUSE_RELEASED)
        {
            return;
        }

        int button = e.getButton();
        if (button != MouseEvent.BUTTON1 && button != MouseEvent.BUTTON3)
        {
            return;
        }

        processMouseReleased(e);    
    }

    private void processMouseReleased(MouseEvent e)
    {
        mouseLock.lock();
        try 
        {
            if (!wasDownMouseEventCurrentSeen)
            {
                // User didn't catch the down event in time, so save it
                // until they ask for it
                downMouseEventPrevious = downMouseEventCurrent;
            }
            downMouseEventCurrent = null;
        }
        finally 
        {
            mouseLock.unlock();
        }
    }

    // ObjectLandPanel calls this when user asks whether left or right is pressed
    public boolean isMouseButtonPressed(int button)
    {
        boolean ret = false;

        mouseLock.lock();
        try 
        {
            if (downMouseEventCurrent != null)
            {
                // Mouse is down now
                ret = (downMouseEventCurrent.getButton() == button);
                if (ret)
                {
                    wasDownMouseEventCurrentSeen = true;
                }
            }
            else if (downMouseEventPrevious != null)
            {
                // Mouse is up, but there was a down event we'd missed
                ret = (downMouseEventPrevious.getButton() == button);
                if (ret)
                {
                    // Cached event has been seen, so stop caching it
                    downMouseEventPrevious = null;
                }
            }
        }
        finally 
        {
            mouseLock.unlock();
        }

        return ret;
    }
}