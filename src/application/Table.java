package application;

/**
 * @author siyuan_zhao
 * @date 2022/11/21 - 12:44
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import javafx.application.Application;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Table extends Application implements Runnable {


  Socket socket = null;
  DataInputStream in = null;
  DataOutputStream out = null;
  Thread thread;
  boolean isConnect;
  String result;
  private Cell[][] cells = new Cell[3][3];
  private char whoseTurn = 'X';
  private Label lblStatus = new Label("X's turn to play");
  private Button connection = new Button("连接");
  private Label gameset = new Label("Game");
  private ArrayList<String> xlist;
  private ArrayList<String> olist;
  private String rank = "r";

  public static void main(String[] args) {

    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    GridPane pane = new GridPane();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        pane.add(cells[i][j] = new Cell(), j, i);
      }
    }
    xlist = new ArrayList<>();
    olist = new ArrayList<>();
//    Button rePlay = new Button("再来一局");
    BorderPane borderPaneForButton = new BorderPane();
    borderPaneForButton.setCenter(lblStatus);
    borderPaneForButton.setRight(gameset);
    borderPaneForButton.setLeft(connection);
    lblStatus.setPadding(new Insets(5));

    BorderPane borderPane = new BorderPane();
    borderPane.setCenter(pane);
    borderPane.setBottom(borderPaneForButton);

//    rePlay.setOnAction(event -> {
//      pane.getChildren().clear();
//      for (int i = 0; i < 3; i++) {
//        for (int j = 0; j < 3; j++) {
//          pane.add(cells[i][j] = new Cell(), j, i);
//        }
//      }
//      xlist.clear();
//      olist.clear();
//      whoseTurn = 'X';
//      lblStatus.setText("X's turn to play");
//    });
    connection.setOnAction(event ->{
          connectAction();
        }
    );
    Scene scene = new Scene(borderPane, 450, 460);
    primaryStage.setScene(scene);
    primaryStage.setTitle("Tic toc toe");
    primaryStage.show();
    //为当前窗口添加关闭监听
    primaryStage.setOnCloseRequest(event -> {
      //对话框 Alert Alert.AlertType.CONFIRMATION：反问对话框
      Alert alert2 = new Alert(Alert.AlertType.CONFIRMATION);
      //设置对话框标题
      alert2.setTitle("Exit");
      //设置内容
      alert2.setHeaderText("确定退出游戏吗？");
      //显示对话框
      Optional<ButtonType> result = alert2.showAndWait();
      //如果点击OK
      if (result.get() == ButtonType.OK){
        // ... user chose OK
        primaryStage.close();
        System.exit(0);
        //否则
      } else {
        event.consume();
      }
    });
  }

  public boolean doConnect() {
    try {
      socket = new Socket();
      InetAddress address = InetAddress.getByName("127.0.0.1");
      InetSocketAddress socketAddress = new InetSocketAddress(address, 8888);
      socket.connect(socketAddress);  //建立连接
      in = new DataInputStream(socket.getInputStream());
      out = new DataOutputStream(socket.getOutputStream());
      return true;
    } catch (IOException e) {
      System.out.println("服务器断开");
      e.printStackTrace();
    }
    return false;
  }


  public Boolean isServerClose(Socket socket) {
    try {
      socket.sendUrgentData(0xFF);//发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
      return false;
    } catch (Exception se) {
      return true;
    }
  }


  public void connectAction() {
      if (!isServerClose(socket)) {
        try {
          socket.close();
          System.out.println("正在断开连接" +"\n" + socket.isClosed());
          isConnect = false;

        } catch (IOException ioException) {
          ioException.printStackTrace();
        }
        connection.setText("connect");
      } else {
        if (doConnect()) {
          thread = new Thread(this);
          thread.start();
          System.out.println("已连接服务器");
          connection.setText("disconnect");

          isConnect = true;
        }
      }
  }

  public void run() {
    System.out.println("启动监听");
    while (true) {
      System.out.println("...");
      if (isServerClose(socket)){
        System.out.println("服务器断开");
        break;
      } else {
        try {
          result = in.readUTF();  //堵塞状态，除非读取到信息
          System.out.println("收到服务器应答" + result);
          if (result.contains("rank")){
            rank = result.substring(result.length()-1);
            if (rank.equals("X")){
              System.out.println("等待对手......");

            Platform.runLater(() ->{
              lblStatus.setText("you are "+ rank+ "  waiting...");
            });
            }else {
              Platform.runLater(() ->{
                lblStatus.setText("you are "+ rank);
              });
            }
          }else if (result.contains("won")){
            Platform.runLater(() ->{
              lblStatus.setText("Game Over");
            });
            whoseTurn = ' ';
          }else if (result.contains("fill")){
            Platform.runLater(() ->{
              lblStatus.setText("Draw! The game is over!");
            });
            whoseTurn = ' ';
          }else if (result.contains("leave")){
            Platform.runLater(() ->{
              lblStatus.setText("Your opponent quit the game");
            });
            whoseTurn = ' ';
          }else if (result.contains("set")){
            String gset = result.split(":")[1];
            Platform.runLater(() -> {
              gameset.setText("Game: "+ gset + " you are " + rank);
              lblStatus.setText("Game start");
            });
          }
          else {
            if (result.split(";").length == 2) {
              String xstr = result.split(";")[0];
              String ostr = result.split(";")[1];
              String[] xq = xstr.split(",");
              String[] oq = ostr.split(",");
              xlist.addAll(Arrays.asList(xq));
              olist.addAll(Arrays.asList(oq));
              for (String str : xlist) {
                int x = Integer.parseInt(str.substring(0, 1));
                int y = Integer.parseInt(str.substring(1));
                if (cells[x][y].getToken() == ' ' && whoseTurn != ' ') {
                  whoseTurn = (whoseTurn == 'X') ? 'O' : 'X';
                  Platform.runLater(() ->{
                    cells[x][y].setToken('X');
                    lblStatus.setText(whoseTurn + "'s turn");
                  });
                }
              }
              for (String str : olist) {
                int x = Integer.parseInt(str.substring(0, 1));
                int y = Integer.parseInt(str.substring(1));
                if (cells[x][y].getToken() == ' ' && whoseTurn != ' ') {
                  whoseTurn = (whoseTurn == 'X') ? 'O' : 'X';
                  Platform.runLater(() ->{
                    cells[x][y].setToken('O');
                    lblStatus.setText(whoseTurn + "'s turn");
                  });
                }

              }

            }else {//first
              String xstr = result.split(";")[0];
              String[] xq = xstr.split(",");
              xlist.addAll(Arrays.asList(xq));
              for (String str : xlist) {
                int x = Integer.parseInt(str.substring(0, 1));
                int y = Integer.parseInt(str.substring(1));
                if (cells[x][y].getToken() == ' '  && whoseTurn != ' ') {
                  whoseTurn = (whoseTurn == 'X') ? 'O' : 'X';
                  Platform.runLater(() ->{
                    cells[x][y].setToken('X');
                    lblStatus.setText(whoseTurn + "'s turn");
                  });
                }
              }
            }
          }
        } catch (IOException e) {
          break;
        }
      }
    }
  }

  public boolean isFill() {
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (cells[i][j].getToken() == ' ') {
          return false;
        }

      }
    }
    return true;
  }

  public boolean isWon(char token) {
    for (int i = 0; i < 3; i++) {
      if (cells[i][0].getToken() == token
          && cells[i][1].getToken() == token
          && cells[i][2].getToken() == token) {
        return true;
      }
    }
    for (int i = 0; i < 3; i++) {
      if (cells[0][i].getToken() == token
          && cells[1][i].getToken() == token
          && cells[2][i].getToken() == token) {
        return true;
      }
    }
    if (cells[0][0].getToken() == token
        && cells[1][1].getToken() == token
        && cells[2][2].getToken() == token) {
      return true;
    }
    if (cells[2][0].getToken() == token
        && cells[1][1].getToken() == token
        && cells[0][2].getToken() == token) {
      return true;
    }

    return false;
  }

  private class Cell extends Pane {

    private char token = ' ';

    //constructor
    public Cell() {
      setStyle(" -fx-border-color: black");
      this.setPrefSize(2000, 2000);
      this.setOnMouseClicked(event -> {
        try {
          handleMouseClick();
        } catch (IOException e) {
          System.out.println("服务器断开");
          e.printStackTrace();
        }
      });
    }

    //getter and setter
    public char getToken() {
      return token;
    }


    public void setToken(char c) {
      token = c;

      if (token == 'X') {
        Line line1 = new Line(10, 10, this.getWidth() - 10, this.getHeight() - 10);
        line1.endXProperty().bind(this.widthProperty().subtract(10));
        line1.endYProperty().bind(this.heightProperty().subtract(10));
        Line line2 = new Line(10, this.getHeight() - 10, this.getWidth() - 10, 10);
        line2.startYProperty().bind(this.heightProperty().subtract(10));
        line2.endXProperty().bind(this.widthProperty().subtract(10));

        this.getChildren().addAll(line1, line2);

      } else if (token == 'O') {
        Ellipse ellipse = new Ellipse(this.getWidth() / 2, this.getHeight() / 2,
            this.getWidth() / 2 - 10, this.getHeight() / 2 - 10);
        ellipse.centerXProperty().bind(this.widthProperty().divide(2));
        ellipse.centerYProperty().bind(this.heightProperty().divide(2));
        ellipse.radiusXProperty().bind(this.widthProperty().divide(2).subtract(10));
        ellipse.radiusYProperty().bind(this.heightProperty().divide(2).subtract(10));
        ellipse.setFill(javafx.scene.paint.Color.ORANGE);
        ellipse.setStroke(Color.BLACK);

        this.getChildren().add(ellipse);
      }

    }

    private void handleMouseClick() throws IOException {
      if (rank.charAt(0) == whoseTurn) {
        if (token == ' ' && whoseTurn != ' ') {

          setToken(whoseTurn);
          StringBuilder xs = new StringBuilder();
          StringBuilder os = new StringBuilder();

          for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
              if (cells[i][j].getToken() == 'X') {
                String temp = i + Integer.toString(j);
                xs.append(temp).append(",");
              } else if (cells[i][j].getToken() == 'O') {
                String temp = i + Integer.toString(j);
                os.append(temp).append(",");
              }
            }
          }
          String s ;
          if (os.length() == 0){
            s = xs.append(";").toString();
            s = s.substring(0,s.length()-2) + s.substring(s.length()-1);
          }else {
            s = xs.deleteCharAt(xs.length()-1).append(";").append(os).toString();
            s = s.substring(0, s.length() - 1);
          }
          out.writeUTF(s);

        }
        if (isWon(whoseTurn)) {
          lblStatus.setText("The game is over!");
          whoseTurn = ' ';
          out.writeUTF("won");
        } else if (isFill()) {
          lblStatus.setText("Draw! The game is over!");
          whoseTurn = ' ';
          out.writeUTF("fill");
        } else {
          whoseTurn = (whoseTurn == 'X') ? 'O' : 'X';
          lblStatus.setText(whoseTurn + "'s turn");
        }
      }
    }
  }
}

