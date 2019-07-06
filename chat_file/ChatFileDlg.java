package chat_file;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.jnetpcap.PcapIf;

@SuppressWarnings("serial")
public class ChatFileDlg extends JFrame implements BaseLayer{

	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	BaseLayer UnderLayer;

	private static LayerManager m_LayerMgr = new LayerManager();

	private JTextField ChattingWrite;

	Container contentPane;

	JTextArea ChattingArea; // Chatting이 뜨는 창
	JTextArea srcAddress; // 근원지 주소
	JTextArea dstAddress; // 도착지 주소
	JTextArea filePath; //파일경로

	JLabel lblsrc;
	JLabel lbldst;
	JLabel lblnic; // NIC라벨 선언

	JButton Setting_Button; // setting button GUI
	JButton Chat_send_Button; // Send button GUI
	JButton File_Select_Button; //파일 선택 버튼
	JButton File_Send_Button; //파일 전송 버튼 
	
	JProgressBar progressBar ;

	static JComboBox<String> NICComboBox; // NIC콤보박스 선언

	int adapterNumber = 0;

	String Text;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		m_LayerMgr.AddLayer(new NILayer("NI")); // NI 레이어 생성
		m_LayerMgr.AddLayer(new EthernetLayer("Ethernet")); // 이더넷 레이어 생성
		m_LayerMgr.AddLayer(new ChatAppLayer("Chat")); // 쳇앱 레이어 생성
		m_LayerMgr.AddLayer(new FileAppLayer("File"));
		m_LayerMgr.AddLayer(new ChatFileDlg("GUI"));
		m_LayerMgr.ConnectLayers(" NI ( *Ethernet ( *Chat ( *GUI ) *File ( *GUI ) ) )"); // 생성한 레이어들을 연결한다.
	}

	public ChatFileDlg(String pName) {
		pLayerName = pName;

		setTitle("Chat_File_Transfer"); // Title을 IPC로 설정
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(250, 250, 644, 425);
		contentPane = new JPanel();
		((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		

		JPanel FilePanel = new JPanel();// FileTransfer panel
		
		FilePanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "file_transfer", // chatting 칸을
																											// 만든다.
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		FilePanel.setBounds(10, 280, 360, 95);
		contentPane.add(FilePanel);
		FilePanel.setLayout(null);
	
		//여기부터 파일 경로 박스 생성
		JPanel filePathPanel = new JPanel();
		filePathPanel .setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		filePathPanel .setBounds(10, 20, 250, 20);
		FilePanel.add(filePathPanel );
		filePathPanel .setLayout(null);

		filePath = new JTextArea();
		filePath.setBounds(2, 2, 250, 20);
		filePathPanel.add(filePath);
		filePath.setLayout(null);
		
		//프로그레스바 생성
		progressBar = new JProgressBar();
		progressBar.setBounds(10, 50, 250, 20);
		progressBar.setMinimum(0); //최소값 0
		progressBar.setMaximum(100); //최대값 100
		progressBar.setValue(0); //0으로 초기화
 		progressBar.setStringPainted(true);//% 설정
		FilePanel.add(progressBar);
		progressBar.setLayout(null);
	
		//여기까지 파일 경로 박스 생성
		
		
		
		///
		JPanel chattingPanel = new JPanel();// chatting panel
		chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting", // chatting 칸을
																											// 만든다.
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
		
		

		
		
		//setting칸을 만든다.
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

		lblsrc = new JLabel("Source Mac Address"); //라벨 추가
		lblsrc.setBounds(10, 75, 170, 20);
		settingPanel.add(lblsrc);

		lblnic = new JLabel("NIC 선택"); //라벨 추가
		lblnic.setBounds(10, 20, 100, 20);
		settingPanel.add(lblnic); // NIC 라벨 생성

		List<PcapIf> list = ((NILayer) (m_LayerMgr.GetLayer("NI"))).m_pAdapterList;
		String[] str = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			str[i] = list.get(i).getDescription();
		} // 리스트의 description 복사

		NICComboBox = new JComboBox<>(str); // 복사한 리스트 이용해서 콤보박스 생성
		NICComboBox.setBounds(10, 40, 170, 20);
		NICComboBox.addActionListener(new setAddressListener());
		settingPanel.add(NICComboBox);

		srcAddress = new JTextArea();
		srcAddress.setBounds(2, 2, 170, 20);
		sourceAddressPanel.add(srcAddress);// src address

		JPanel destinationAddressPanel = new JPanel();
		destinationAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		destinationAddressPanel.setBounds(10, 212, 170, 20);
		settingPanel.add(destinationAddressPanel);
		destinationAddressPanel.setLayout(null);

		lbldst = new JLabel("Destination Mac Address");
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
		
		File_Select_Button = new JButton("File...");
		File_Select_Button.setBounds(270, 20, 80, 20);
		File_Select_Button.addActionListener(new setAddressListener());
		FilePanel.add(File_Select_Button); //File select Button
		
		File_Send_Button = new JButton("Send");
		File_Send_Button.setBounds(270, 50, 80, 20);
		File_Send_Button.addActionListener(new setAddressListener());
		FilePanel.add(File_Send_Button); //File select Button

		setVisible(true);

	}

	class setAddressListener implements ActionListener {
		@Override
		
		public void actionPerformed(ActionEvent e) {
			
			byte[] mac;
			try {
				mac = ((NILayer) m_LayerMgr.GetLayer("NI")).m_pAdapterList
						.get(NICComboBox.getSelectedIndex()).getHardwareAddress();
				
				String _mac = asString(mac);
				srcAddress.setText(_mac);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			if(e.getSource() == File_Select_Button) { //파일선택버튼 눌렀을때
				JFileChooser chooser = new JFileChooser(); //filechooser 객체생성
				int ret = chooser.showOpenDialog(null); //열기창 정의
				if(ret != JFileChooser.APPROVE_OPTION) {
					JOptionPane.showMessageDialog(null, "경로를 선택하지 않았습니다.",
							"경고",JOptionPane.WARNING_MESSAGE);
					return;
				}
				String path = chooser.getSelectedFile().getPath(); //파일경로 임시 저장
				filePath.setText(path); //filePath자리에 세팅
			}
			
			if(e.getSource() == File_Send_Button) { //파일전송버튼 눌렀을때
				if(filePath.getText().isEmpty()) { //파일경로 비어있으면 오류발생
					JOptionPane.showMessageDialog(ChattingArea, "파일 선택 오류");
				} else {
					((FileAppLayer)m_LayerMgr.GetLayer("File")).Send(filePath.getText()); //찍힌 경로대로 Send 실행
					((FileAppLayer)m_LayerMgr.GetLayer("File")).Receive();
				}
			}
			
			if (e.getSource() == Setting_Button) { // SettingButton 클릭시 실행

				if (Setting_Button.getText() == "Reset") { // SettingButton 입력이 Reset이면
					srcAddress.setText(""); // 근원지 주소 초기화
					dstAddress.setText(""); // 도착지 주소 초기화

					Setting_Button.setText("Setting"); // Reset으로 변한 Setting버튼을 다시 Setting으로 변경(다시 초기화 할 수 있게 하기 위함
					dstAddress.setEnabled(true); // DstAddress값 변경 가능하게 설정
					srcAddress.setEnabled(true); // SrcAddress값 변경 가능하게 설정
				} else { // 이경우 셋팅을 진행한다.

					((NILayer) m_LayerMgr.GetLayer("NI")).SetAdapterNumber(NICComboBox.getSelectedIndex());
					// 선택한 인덱스로 어뎁터 번호 설정
					 
					
					String srcAdd = srcAddress.getText();	//출발지 주소 저장(16진수에 토큰도껴있음)
					String dstAdd = dstAddress.getText();	//도착지 주소 저장(16진수에 토큰도껴있음)
					
					byte[] _srcAdd = new byte[6];
					byte[] _dstAdd = new byte[6];
					
					for(int i = 0,j=0 ; i < 16 ; i+=3) {	//_srcAdd생성
						String tem = srcAdd.substring(i,i+2);	//문자 2개 자르고
						int te = Integer.parseInt(tem,16);		//자른거 16진수정수로 변환해서
						_srcAdd[j++] = (byte)(te&0xFF);				//바이트배열에 넣는다.
					}	//(0,2)(3,5),(6,8),(9,11),(12,14),(15,17)
					
					for(int i = 0,j=0 ; i < 16 ; i+=3) {	//_dstAdd 생성
						String tem = dstAdd.substring(i,i+2);	//문자 2개 자르고
						int te = Integer.parseInt(tem,16);		//자른거 16진수정수로 변환해서
						_dstAdd[j++] = (byte)(te&0xFF);				//바이트배열에 넣는다.
					}	//(0,2)(3,5),(6,8),(9,11),(12,14),(15,17)
					
					((EthernetLayer)m_LayerMgr.GetLayer("Ethernet")).SetHeader(_srcAdd, _dstAdd);
					//이더넷에 주소 넣어준다.
					
					
					Setting_Button.setText("Reset"); // Setting Button을 Reset BUtton으로 변경
					dstAddress.setEnabled(false); // DstAddress값 변경 못하게 설정
					srcAddress.setEnabled(false); // SrcAddress값 변경 못하게 설정
				}
			}

			
			
			if (e.getSource() == Chat_send_Button) {

				if (Setting_Button.getText() == "Reset") { // Setting Button이 Reset인지 확인
					// (Reset이 아니라면 Setting이 안되어있다는 뜻이기 때문에)
					ChattingArea.append("[SEND] " + ChattingWrite.getText() + "\n");
					// Chatting Write에 적은Text를 ChattingArea에 보여준다.
					
					((ChatAppLayer) m_LayerMgr.GetLayer("Chat")).Send(ChattingWrite.getText().getBytes(),
							ChattingWrite.getText().getBytes().length);
					// 쳇앱 레이어에서 문자정보를 보낸다.(매개변수로 Text를 byte로 변환한것과 그 길이를 넣어준다.)
					((ChatAppLayer) m_LayerMgr.GetLayer("Chat")).Receive();
					// Send를 했으니 Receive 실행
					
				} else {
					JOptionPane.showMessageDialog(ChattingArea, "주소 설정 오류");
				} // Setting이 안된상태라면 주소설정 오류 메시지를 띄운다.
			}
		}
	}

	public boolean ReceiveChat(byte[] input) {

		ChattingArea.append("[RECV] " + new String(input) + "\n");
		// input으로 들어온 byte를 String으로 바꿔서 보여준다.
		return true;

	}
	public boolean ReceiveFile(String fileName, byte[] data) {
		
		//File file = new File(".\\"+fileName);
		//File file = new File(".//" , fileName);
//		File file = new File(".\\"+fileName);
//		File file = new File(Paths.get(fileName));
		
//		try {
////            FileOutputStream os = new FileOutputStream(file);
//            FileOutputStream os = new FileOutputStream(fileName);
//            os.write(data);
//            os.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
		fileName = fileName.trim(); //공백 제거
		try {
			Files.write(Paths.get(fileName),data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) { // 하위 레이어 설정
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) { // 상위 레이어 설정
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
		// nUpperLayerCount++;
	}

	@Override
	public String GetLayerName() { // 레이어 이름 반환
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() { // 하위 레이어 반환
		// TODO Auto-generated method stub
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) { // 상위 레이어 반환
		// TODO Auto-generated method stub
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) { // 매개변수로 받은 레이어를 상위레이어로 세팅하고, 그 레이어의 하위를 함수를 호출한 객체로 저장
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);

	}

	private static String asString(final byte[] mac) {
		// 6바이트 형태의 mac을 문자열로 변경하는 함수
		final StringBuilder buf = new StringBuilder();
		for (byte b : mac) {
			if (buf.length() != 0) {
				buf.append("-");
			}
			if (b >= 0 && b < 16) {
				buf.append('0');// 16미만일시 한자릿수 출력
			}
			buf.append(Integer.toHexString((b < 0) ? b + 256 : b).toUpperCase());

		}
		return buf.toString();
	}
	
	
	public File makeFileFromBytes(String fileName , byte[] data) { //데이터를 파일로 만들어준다.
		File file = new File("./"+fileName);
		try {
            OutputStream os = new FileOutputStream(file);
            os.write(data);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
		return file;
	}
}
