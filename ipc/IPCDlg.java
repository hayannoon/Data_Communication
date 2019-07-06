package ipc;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class IPCDlg extends JFrame implements BaseLayer {

	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	BaseLayer UnderLayer;

	private static LayerManager m_LayerMgr = new LayerManager();

	private JTextField ChattingWrite;

	Container contentPane;

	JTextArea ChattingArea;		//Chatting이 뜨는 창
	JTextArea srcAddress;		//근원지 주소
	JTextArea dstAddress;		//도착지 주소
	
	JLabel lblsrc;
	JLabel lbldst;
	
	JButton Setting_Button;		//setting button GUI
	JButton Chat_send_Button;	//Send button GUI
	
	static JComboBox<String> NICComboBox;
	
	int adapterNumber = 0;
	
	String Text;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		m_LayerMgr.AddLayer(new SocketLayer("Socket"));	//소켓 레이어 생성
		m_LayerMgr.AddLayer(new ChatAppLayer("Chat"));	//쳇앱 레이어 생성
		m_LayerMgr.AddLayer(new IPCDlg("GUI"));		//Dialog 레이어 생성

		m_LayerMgr.ConnectLayers(" Socket ( *Chat ( *GUI ) ) ");	//생성한 레이어들을 연결한다.
	}
	
	public IPCDlg(String pName) {
		pLayerName = pName;

		setTitle("IPC");	//Title을 IPC로 설정
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(250, 250, 644, 425);
		contentPane = new JPanel();
		((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JPanel chattingPanel = new JPanel();// chatting panel
		chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting",	//chatting 칸을 만든다.
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		chattingPanel.setBounds(10, 5, 360, 276);
		contentPane.add(chattingPanel);
		chattingPanel.setLayout(null);

		JPanel chattingEditorPanel = new JPanel();// chatting write panel
		chattingEditorPanel.setBounds(10, 15, 340, 210);
		chattingPanel.add(chattingEditorPanel);
		chattingEditorPanel.setLayout(null);

		ChattingArea = new JTextArea();
		ChattingArea.setEditable(false);
		ChattingArea.setBounds(0, 0, 340, 210);
		chattingEditorPanel.add(ChattingArea);// chatting edit		

		JPanel chattingInputPanel = new JPanel();// chatting write panel
		chattingInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		chattingInputPanel.setBounds(10, 230, 250, 20);
		chattingPanel.add(chattingInputPanel);
		chattingInputPanel.setLayout(null);

		ChattingWrite = new JTextField();
		ChattingWrite.setBounds(2, 2, 250, 20);// 249
		chattingInputPanel.add(ChattingWrite);
		ChattingWrite.setColumns(10);// writing area

		JPanel settingPanel = new JPanel();
		settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "setting",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		settingPanel.setBounds(380, 5, 236, 371);
		contentPane.add(settingPanel);
		settingPanel.setLayout(null);

		JPanel sourceAddressPanel = new JPanel();
		sourceAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		sourceAddressPanel.setBounds(10, 96, 170, 20);
		settingPanel.add(sourceAddressPanel);
		sourceAddressPanel.setLayout(null);

		lblsrc = new JLabel("Source Address");
		lblsrc.setBounds(10, 75, 170, 20);
		settingPanel.add(lblsrc);

		srcAddress = new JTextArea();
		srcAddress.setBounds(2, 2, 170, 20);
		sourceAddressPanel.add(srcAddress);// src address

		JPanel destinationAddressPanel = new JPanel();
		destinationAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		destinationAddressPanel.setBounds(10, 212, 170, 20);
		settingPanel.add(destinationAddressPanel);
		destinationAddressPanel.setLayout(null);

		lbldst = new JLabel("Destination Address");
		lbldst.setBounds(10, 187, 190, 20);
		settingPanel.add(lbldst);

		dstAddress = new JTextArea();
		dstAddress.setBounds(2, 2, 170, 20);
		destinationAddressPanel.add(dstAddress);// dst address

		Setting_Button = new JButton("Setting");// setting
		Setting_Button.setBounds(80, 270, 100, 20);
		Setting_Button.addActionListener(new setAddressListener());
		settingPanel.add(Setting_Button);// setting

		Chat_send_Button = new JButton("Send");
		Chat_send_Button.setBounds(270, 230, 80, 20);
		Chat_send_Button.addActionListener(new setAddressListener());
		chattingPanel.add(Chat_send_Button);// chatting send button

		setVisible(true);

	}

	class setAddressListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if(e.getSource() == Setting_Button) {		//SettingButton 클릭시 실행
					
				if(Setting_Button.getText() == "Reset") {	//SettingButton 입력이 Reset이면
					srcAddress.setText("");	//근원지 주소 초기화
					dstAddress.setText("");	//도착지 주소 초기화
					
					
					Setting_Button.setText("Setting");	//Reset으로 변한 Setting버튼을 다시 Setting으로 변경(다시 초기화 할 수 있게 하기 위함
					dstAddress.setEnabled(true);		//DstAddress값 변경 가능하게 설정
					srcAddress.setEnabled(true);		//SrcAddress값 변경 가능하게 설정
				} else {
					
					String Ssrc = srcAddress.getText();	//근원지 주소 저장
					String Sdst = dstAddress.getText();	//도착지 주소 저장
					
						
					int src = Integer.parseInt(Ssrc);	//String형태인 근원지 주소를 int형으로 변환
					int dst = Integer.parseInt(Sdst);	//String형태인 도착지 주소를 int형으로 변환
					
					
					((SocketLayer) m_LayerMgr.GetLayer("Socket")).setClientPort(dst);	//DstAddress의 text를 SocketLayer Client port에 저장
					((SocketLayer) m_LayerMgr.GetLayer("Socket")).setServerPort(src);	//SrcAddress의 text를 SocketLayer Server port에 저장
					
					
					((ChatAppLayer) m_LayerMgr.GetLayer("Chat")).SetEnetSrcAddress(src);	//SrcAddress의 text를 ChatAppLayer header에 저장
					((ChatAppLayer) m_LayerMgr.GetLayer("Chat")).SetEnetDstAddress(dst);	//SrcAddress의 text를 ChatAppLayer header에 저장
					
					((SocketLayer) m_LayerMgr.GetLayer("Socket")).Receive();	//SocketLayer thread 실행 - SocketLayer의 서버를 실행시킴
					
					Setting_Button.setText("Reset");	//Setting Button을 Reset BUtton으로 변경
					dstAddress.setEnabled(false);		//DstAddress값 변경 못하게 설정
					srcAddress.setEnabled(false);		//SrcAddress값 변경 못하게 설정
				}
			}
			
			if(e.getSource() == Chat_send_Button) {
				
				if(Setting_Button.getText() == "Reset") {	//Setting Button이 Reset인지 확인
					//(Reset이 아니라면 Setting이 안되어있다는 뜻이기 때문에)
					ChattingArea.append("[SEND] " + ChattingWrite.getText()+"\n");
					//Chatting Write에 적은Text를 ChattingArea에 보여준다.
					
					((ChatAppLayer) m_LayerMgr.GetLayer("Chat")).Send(ChattingWrite.getText().getBytes(), ChattingWrite.getText().getBytes().length);
					//쳇앱 레이어에서 문자정보를 보낸다.(매개변수로 Text를 byte로 변환한것과 그 길이를 넣어준다.)
					((ChatAppLayer) m_LayerMgr.GetLayer("Chat")).Receive();
					//Send를 했으니 Receive 실행
					
				} else {
					JOptionPane.showMessageDialog(ChattingArea, "주소 설정 오류");
				}	//Setting이 안된상태라면 주소설정 오류 메시지를 띄운다.
			}
		}
	}

	public boolean Receive(byte[] input) {	
		
		ChattingArea.append("[RECV] " + new String(input) +"\n");
		//input으로 들어온 byte를 String으로 바꿔서 보여준다.
		return true;
		
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {	//하위 레이어 설정
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {	//상위 레이어 설정
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
		// nUpperLayerCount++;
	}

	@Override
	public String GetLayerName() {	//레이어 이름 반환
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {	//하위 레이어 반환
		// TODO Auto-generated method stub
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {	//상위 레이어 반환
		// TODO Auto-generated method stub
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {	//매개변수로 받은 레이어를 상위레이어로 세팅하고, 그 레이어의 하위를 함수를 호출한 객체로 저장
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);

	}

}
