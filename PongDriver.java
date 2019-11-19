import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
public class PongDriver extends JFrame
{
   private int fps,ups;
   private boolean running=true,paused=false;
   private PongPanel gamePanel=new PongPanel();
   private PongDriver()
   {
      super("Pong");
      pack();
      setSize(700,700);
      setLocationRelativeTo(null);
      setDefaultCloseOperation(EXIT_ON_CLOSE);
      setContentPane(gamePanel);
      setVisible(true);
      gamePanel.requestFocus();
      runGameLoop();
   }
   private void runGameLoop()
   {
      Thread game=
         new Thread()
         {
            public void run()
            {
               gameLoop();
            } 
         };
      game.run();
   }
   private void gameLoop()
   {
      final int GAME_HERTZ=30;
      final long TIME_BETWEEN_UPDATES=1000000000/GAME_HERTZ;
      final int TARGET_FPS=60;
      final long TARGET_FRAME_TIME=1000000000/TARGET_FPS;
      long prevTime=System.nanoTime();
      long elapsed=0;
      //used to calculate FPS
      int frames=0;
      long runningFrameTime=0;
      //used to calculate UPS
      int updates=0;
      long runningUpdateTime=0;
      long prevUpdateTime=System.nanoTime();
      while(running)
      {
         long now=System.nanoTime();
         elapsed+=now-prevTime;
         runningFrameTime+=now-prevTime;
         frames++;
         while(elapsed>=TIME_BETWEEN_UPDATES)
         {
            gamePanel.update();
            elapsed-=TIME_BETWEEN_UPDATES;
            runningUpdateTime+=System.nanoTime()-prevUpdateTime;
            updates++;
            prevUpdateTime=System.nanoTime();
         }
         double interpolation=(double)(System.nanoTime()-prevTime)/TIME_BETWEEN_UPDATES;
         //System.out.println(interpolation);
         prevTime=now;
         if(paused)
            interpolation=0;
         gamePanel.render(interpolation);
         
         if(runningUpdateTime>=1000000000)
         {
            ups=updates;
            updates=0;
            runningUpdateTime=0;
         }
         if(runningFrameTime>=1000000000)
         {
            fps=frames;
            frames=0;
            runningFrameTime=0;
         }
         //    Below is other option to following while loop, but Thread.sleep(int) less reliable than Thread.yield()
         //try{Thread.sleep((TARGET_FRAME_TIME-System.nanoTime()+prevTime)/1000000);}catch(Exception e){e.printStackTrace();}
         while(now-prevTime<TARGET_FRAME_TIME && now-prevTime<TIME_BETWEEN_UPDATES)
         {
            Thread.yield();
            now=System.nanoTime();
         }
      }
   }
   public static void main(String[]arg)
   {
      new PongDriver();
   }
   private class PongPanel extends JPanel
   {
      private final String PAUSE="pause"
                           ,MOVE_UP_1="move up 1"
                           ,MOVE_DOWN_1="move down 1"
                           ,STOP_1="stop 1"
                           ,MOVE_UP_2="move up 2"
                           ,MOVE_DOWN_2="move down 2"
                           ,STOP_2="stop 2";
      private GameObject[] gameObjects=new GameObject[3];
      private boolean initial=true;
      private PongPanel()
      {
         gameObjects[0]=new GameObject();
         gameObjects[1]=new Player();
         gameObjects[2]=new Player();
         setBackground(Color.GRAY.darker());
         setKeyBindings();
         setFocusable(true);
      }
      private void gimp(String key,String name)
      {
         getInputMap().put(KeyStroke.getKeyStroke(key),name);
      }
      private void gamp(String name,AbstractAction action)
      {
         getActionMap().put(name,action);
      }
      private void setKeyBindings()
      {
         gimp("ESCAPE",PAUSE);
         gimp("W",MOVE_UP_1);
         gimp("S",MOVE_DOWN_1);
         gimp("UP",MOVE_UP_2);
         gimp("DOWN",MOVE_DOWN_2);
         gimp("released W",STOP_1);
         gimp("released S",STOP_1);
         gimp("released UP",STOP_2);
         gimp("released DOWN",STOP_2);
         
         gamp(PAUSE,new MenuEvent(0));
         gamp(MOVE_UP_1,new MoveEvent1(0));
         gamp(MOVE_DOWN_1,new MoveEvent1(1));
         gamp(STOP_1,new MoveEvent1(2));
         gamp(MOVE_UP_2,new MoveEvent2(0));
         gamp(MOVE_DOWN_2,new MoveEvent2(1));
         gamp(STOP_2,new MoveEvent2(2));
      }
      private void update()
      {
         if(initial)
         {
            paused=true;
            int width=getWidth();
            int height=getHeight();
            gameObjects[0].setPosition((width-gameObjects[0].WIDTH)/2,(height-gameObjects[0].HEIGHT)/2);
            gameObjects[0].SPEED=10;
            gameObjects[0].xSpeed=((int)(Math.random()*2)*2-1)*gameObjects[0].SPEED;
            gameObjects[0].ySpeed=0;
            gameObjects[1].setPosition(50,(height-gameObjects[1].HEIGHT)/2);
            gameObjects[2].setPosition(width-50,(height-gameObjects[2].HEIGHT)/2);
            initial=false;
         }
         for(GameObject gameObject:gameObjects)
         {
            int scored=gameObject.update(getWidth(),getHeight());
            if(scored>0)
            {
               initial=true;
               ((Player)gameObjects[scored]).points++;
            }
         }
         checkFutureCollisions();
      }
      private void checkFutureCollisions()
      {
         for(int i=1;i<gameObjects.length;i++)
            if(gameObjects[0].futureHitBox.intersects(gameObjects[i].futureHitBox)) 
            { 
               gameObjects[0].collide(gameObjects[i].futureHitBox);
               return;
            }
      }
      private void render(double interpolation)
      {
         for(GameObject gameObject:gameObjects)
            gameObject.interpolate(interpolation);
         repaint();
      }
      @Override
      public void paintComponent(Graphics g)
      {
         super.paintComponent(g);
         for(GameObject gameObject:gameObjects)
            if(gameObject instanceof Player)
               ((Player)gameObject).draw(g);
            else
               gameObject.draw(g);
         g.drawString("UPS: "+ups,getWidth()/2-45,getHeight()-75);
         g.drawString("FPS: "+fps,getWidth()/2-45,getHeight()-50);
         if(paused)
         {
            g.setColor(Color.RED);
            g.setFont(new Font(Font.DIALOG_INPUT,Font.BOLD,75));
            g.drawString("PAUSED",getWidth()/2-135,100);
         }
      }
      //Player 1, on left, W/S controls
      private class MoveEvent1 extends AbstractAction
      {
         final int direction;
         Player p1=(Player)gameObjects[1];
         private MoveEvent1(int dir)
         {
            direction=dir;
         }
         public void actionPerformed(ActionEvent e)
         {
            if(direction==0)
               p1.ySpeed=-p1.SPEED;
            else if(direction==1)
               p1.ySpeed=p1.SPEED;
            else if(direction==2)
               p1.ySpeed=0;
         }
      }
      //Player 2, on right, up/down controls
      private class MoveEvent2 extends AbstractAction
      {
         final int direction;
         Player p2=(Player)gameObjects[2];
         private MoveEvent2(int dir)
         {
            direction=dir;
         }
         public void actionPerformed(ActionEvent e)
         {
            if(direction==0)
               p2.ySpeed=-p2.SPEED;
            else if(direction==1)
               p2.ySpeed=p2.SPEED;
            else if(direction==2)
               p2.ySpeed=0;
         }
      }
   }
   private class MenuEvent extends AbstractAction
   {
      final int code;
      private MenuEvent(int key)
      {
         code=key;
      }
      public void actionPerformed(ActionEvent e)
      {
         if(code==0)
            paused=!paused;
      }
   }
   private class GameObject
   {
      protected final int WIDTH,HEIGHT;
      protected int SPEED;
      protected int x,y,xSpeed,ySpeed;
      protected Rectangle futureHitBox;
      private GameObject()
      {
         WIDTH=50;
         HEIGHT=50;
         SPEED=10;
         futureHitBox=new Rectangle(x,y,WIDTH,HEIGHT);
      }
      private GameObject(int width,int height,int speed)
      {
         WIDTH=width;
         HEIGHT=height;
         SPEED=speed;
         futureHitBox=new Rectangle(x,y,WIDTH,HEIGHT);
      }
      protected int update(int width,int height)
      {
         int scored=0;
         if(x+WIDTH>=width)
            scored=1;
         else if(x<=0)
            scored=2;
         if(y+HEIGHT>=height)
            ySpeed=-SPEED;
         else if(y<=0)
            ySpeed=SPEED;
         futureHitBox.setLocation(x+xSpeed,y+ySpeed);
         return scored;
      }
      protected void interpolate(double interpolation)
      {
         x+=xSpeed*interpolation;
         y+=ySpeed*interpolation;
      }
      protected void draw(Graphics g)
      {
         g.setColor(Color.WHITE);
         g.fillOval(x,y,WIDTH,HEIGHT);
      }
      private void setPosition(int xPos,int yPos)
      {
         x=xPos;
         y=yPos;
      }
      private void collide(Rectangle other)
      {
         Rectangle overlap=futureHitBox.intersection(other);
         int overlapCenterX=(int)(overlap.getX()+overlap.getWidth()/2);
         int overlapCenterY=(int)(overlap.getY()+overlap.getHeight()/2);
         int centerX=(int)futureHitBox.getX()+WIDTH/2;
         int centerY=(int)futureHitBox.getY()+HEIGHT/2;
         if(centerX<overlapCenterX)
            xSpeed=-SPEED;
         else if(centerX>overlapCenterX)
            xSpeed=SPEED;
         else
            xSpeed=0;
         if(centerY<overlapCenterY)
            ySpeed=-SPEED;
         else if(centerY>overlapCenterY)
            ySpeed=SPEED;
         else ySpeed=0;
         SPEED++;
      }
   }
   private class Player extends GameObject
   {
      private int points=0;
      private int scrnWidth,scrnHeight;
      private boolean isP2=false;
      private Player()
      {
         super(25,100,10);
      }
      @Override
      protected int update(int width,int height)
      {
         if(x!=50)
         {
            isP2=true;
            x=width-50;
         }
         scrnWidth=width;
         scrnHeight=height;
         if(y+ySpeed+HEIGHT>=height)
         {
            y=height-HEIGHT;
            ySpeed=0;
         }
         else if(y+ySpeed<=0)
         {
            y=0;
            ySpeed=0;
         }
         futureHitBox.setLocation(x+xSpeed,y+ySpeed);
         return 0;
      }
      @Override
      protected void draw(Graphics g)
      {
         g.setColor(Color.ORANGE);
         g.fillRect(x,y,WIDTH,HEIGHT);
         g.setColor(Color.WHITE);
         g.setFont(new Font(Font.DIALOG_INPUT,Font.PLAIN,20));
         if(isP2)
         {
            g.drawString("Player 2",x-100,scrnHeight-75);
            g.drawString("Score: "+points,x-100,scrnHeight-50);
         }
         else
         {
            g.drawString("Player 1",50,scrnHeight-75);
            g.drawString("Score: "+points,50,scrnHeight-50);
         }
      }
   }
}