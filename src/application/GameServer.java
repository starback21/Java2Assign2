package application;

/**
 * @author siyuan_zhao
 * @date 2022/11/21 - 15:13
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class GameServer {

  static ServerSocket server = null;
  static ServerThread thread;
  static Socket you = null;
  static int cnt = 0;
  static int gamecnt = 0;
  public static List<Socket> socketList = new ArrayList<>();
  public static HashMap<Integer, Socket> socketHashMap = new HashMap<>();

  public static void main(String[] args) {

    while (true) {
      if (server == null) {
        try {
          server = new ServerSocket(8888);//启动监听

          System.out.println("Tic-tac-toe 服务器启动");
          System.out.println("等待......");
        } catch (IOException e1) {
          System.out.println("启动服务器失败");   //ServerSocket对象不能重复创建
          e1.printStackTrace();//捕捉异常栈堆
        }
      }

      if (server != null) {
        try {
          you = server.accept();
          socketList.add(you);
          System.out.println("用户ַ" + you.getPort() + "上线");
          cnt++;

        } catch (IOException e) {
          System.out.println("正在等待");
        }
      }
      if (you != null) {
        //为每个客户启动一个专门的线程
        thread = new ServerThread(you);
        thread.start();
        System.out.println("当前连接用户数量：" + cnt);

      }
    }
  }

}

class ServerThread extends Thread {

  private Socket socket;
  private DataOutputStream out = null;
  private DataInputStream in = null;
  private String answer;
  private int count = 0;


  ServerThread(Socket t) {
    socket = t;
    try {
      in = new DataInputStream(socket.getInputStream());
      out = new DataOutputStream(socket.getOutputStream());
      count = GameServer.cnt;
      String rank = " ";
      if (count % 2 == 0) {
        rank = "O";
        System.out.println(socket.getPort() + "你的rank是" + rank);
        out.writeUTF("rank: " + rank);

        Socket temp1 = GameServer.socketList.get(GameServer.gamecnt);
        Socket temp2 = GameServer.socketList.get(GameServer.gamecnt + 1);
        GameServer.socketHashMap.put(temp1.getPort(), temp2);
        GameServer.socketHashMap.put(temp2.getPort(), temp1);
        GameServer.gamecnt = GameServer.gamecnt + 2;
        DataOutputStream iout = new DataOutputStream(temp1.getOutputStream());
        iout.writeUTF("game set:" + GameServer.gamecnt / 2);
        out.writeUTF("game set:" + GameServer.gamecnt / 2);

      } else {
        rank = "X";
        System.out.println(socket.getPort() + "你的rank是" + rank);
        out.writeUTF("rank: " + rank);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    while (true) {
      boolean isConnect = !isServerClose(socket);
      if (isConnect) {
        try {
          answer = in.readUTF();
          System.out.println("收到来自客户" + socket.getPort() + "的消息" + answer);
          Socket opSocket = GameServer.socketHashMap.get(socket.getPort());
          DataOutputStream iout = new DataOutputStream(opSocket.getOutputStream());
          iout.writeUTF(answer);
        } catch (IOException e) {
          GameServer.cnt--;
          System.out.println("用户离开" + socket.getPort());
          try {
            Socket temp = GameServer.socketHashMap.get(socket.getPort());
            if (temp != null && !isServerClose(temp)) {
              DataOutputStream iout = new DataOutputStream(temp.getOutputStream());
              iout.writeUTF("leave");
            }
          } catch (IOException ex) {
            ex.printStackTrace();
          }
          break;
        }

      } else {
        GameServer.cnt--;
        System.out.println("客户离开:" + socket.getPort());
        break;
      }
    }
  }


  public Boolean isServerClose(Socket socket) {
    try {
      socket.sendUrgentData(0xFF);//发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
      return false;
    } catch (Exception se) {
      return true;
    }
  }
}

