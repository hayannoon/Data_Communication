package stopwait;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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

import org.jnetpcap.PcapIf;

@SuppressWarnings("serial")
public class StopWaitDlg extends JFrame implements BaseLayer {

	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	BaseLayer UnderLayer;

	private static LayerManager m_LayerMgr = new LayerManager();

	private JTextField ChattingWrite;

	Container contentPane;

	JTextArea ChattingArea; // Chatting�� �ߴ� â
	JTextArea srcAddress; // �ٿ��� �ּ�
	JTextArea dstAddress; // ������ �ּ�

	JLabel lblsrc;
	JLabel lbldst;
	JLabel lblnic; // NIC�� ����

	JButton Setting_Button; // setting button GUI
	JButton Chat_send_Button; // Send button GUI

	static JComboBox<String> NICComboBox; // NIC�޺��ڽ� ����

	int adapterNumber = 0;

	String Text;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		m_LayerMgr.AddLayer(new NILayer("NI")); // ���� ���̾� ����
		m_LayerMgr.AddLayer(new EthernetLayer("Ethernet")); // �¾� ���̾� ����
		m_LayerMgr.AddLayer(new ChatAppLayer("Chat")); // Dialog ���̾� ����
		m_LayerMgr.AddLayer(new StopWaitDlg("GUI"));
		m_LayerMgr.ConnectLayers(" NI ( *Ethernet ( *Chat ( *GUI ) ) )"); // ������ ���̾���� �����Ѵ�.
	}

	public StopWaitDlg(String pName) {
		pLayerName = pName;

		setTitle("IPC"); // Title�� IPC�� ����
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(250, 250, 644, 425);
		contentPane = new JPanel();
		((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JPanel chattingPanel = new JPanel();// chatting panel
		chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting", // chatting ĭ��
																											// �����.
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

		lblsrc = new JLabel("Source Mac Address");
		lblsrc.setBounds(10, 75, 170, 20);
		settingPanel.add(lblsrc);

		lblnic = new JLabel("NIC ����");
		lblnic.setBounds(10, 20, 100, 20);
		settingPanel.add(lblnic); // NIC �� ����

		List<PcapIf> list = ((NILayer) (m_LayerMgr.GetLayer("NI"))).m_pAdapterList;
		String[] str = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			str[i] = list.get(i).getDescription();
		} // ����Ʈ�� description ����

		NICComboBox = new JComboBox<>(str); // ������ ����Ʈ �̿��ؼ� �޺��ڽ� ����
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
			
			
			
			if (e.getSource() == Setting_Button) { // SettingButton Ŭ���� ����

				if (Setting_Button.getText() == "Reset") { // SettingButton �Է��� Reset�̸�
					srcAddress.setText(""); // �ٿ��� �ּ� �ʱ�ȭ
					dstAddress.setText(""); // ������ �ּ� �ʱ�ȭ

					Setting_Button.setText("Setting"); // Reset���� ���� Setting��ư�� �ٽ� Setting���� ����(�ٽ� �ʱ�ȭ �� �� �ְ� �ϱ� ����
					dstAddress.setEnabled(true); // DstAddress�� ���� �����ϰ� ����
					srcAddress.setEnabled(true); // SrcAddress�� ���� �����ϰ� ����
				} else { // �̰�� ������ �����Ѵ�.

					((NILayer) m_LayerMgr.GetLayer("NI")).SetAdapterNumber(NICComboBox.getSelectedIndex());
					// ������ �ε����� ��� ��ȣ ����
					 
					
					String srcAdd = srcAddress.getText();	//����� �ּ� ����(16������ ��ū��������)
					String dstAdd = dstAddress.getText();	//������ �ּ� ����(16������ ��ū��������)
					
					byte[] _srcAdd = new byte[6];
					byte[] _dstAdd = new byte[6];
					
					for(int i = 0,j=0 ; i < 16 ; i+=3) {	//_srcAdd����
						String tem = srcAdd.substring(i,i+2);	//���� 2�� �ڸ���
						int te = Integer.parseInt(tem,16);		//�ڸ��� 16���������� ��ȯ�ؼ�
						_srcAdd[j++] = (byte)(te&0xFF);				//����Ʈ�迭�� �ִ´�.
					}	//(0,2)(3,5),(6,8),(9,11),(12,14),(15,17)
					
					for(int i = 0,j=0 ; i < 16 ; i+=3) {	//_dstAdd ����
						String tem = dstAdd.substring(i,i+2);	//���� 2�� �ڸ���
						int te = Integer.parseInt(tem,16);		//�ڸ��� 16���������� ��ȯ�ؼ�
						_dstAdd[j++] = (byte)(te&0xFF);				//����Ʈ�迭�� �ִ´�.
					}	//(0,2)(3,5),(6,8),(9,11),(12,14),(15,17)
					
					((EthernetLayer)m_LayerMgr.GetLayer("Ethernet")).SetHeader(_srcAdd, _dstAdd);
					//�̴��ݿ� �ּ� �־��ش�.
					
					
					Setting_Button.setText("Reset"); // Setting Button�� Reset BUtton���� ����
					dstAddress.setEnabled(false); // DstAddress�� ���� ���ϰ� ����
					srcAddress.setEnabled(false); // SrcAddress�� ���� ���ϰ� ����
				}
			}

			
			
			if (e.getSource() == Chat_send_Button) {

				if (Setting_Button.getText() == "Reset") { // Setting Button�� Reset���� Ȯ��
					// (Reset�� �ƴ϶�� Setting�� �ȵǾ��ִٴ� ���̱� ������)
					ChattingArea.append("[SEND] " + ChattingWrite.getText() + "\n");
					// Chatting Write�� ����Text�� ChattingArea�� �����ش�.
					
					((ChatAppLayer) m_LayerMgr.GetLayer("Chat")).Send(ChattingWrite.getText().getBytes(),
							ChattingWrite.getText().getBytes().length);
					// �¾� ���̾�� ���������� ������.(�Ű������� Text�� byte�� ��ȯ�ѰͰ� �� ���̸� �־��ش�.)
					((ChatAppLayer) m_LayerMgr.GetLayer("Chat")).Receive();
					// Send�� ������ Receive ����
					
				} else {
					JOptionPane.showMessageDialog(ChattingArea, "�ּ� ���� ����");
				} // Setting�� �ȵȻ��¶�� �ּҼ��� ���� �޽����� ����.
			}
		}
	}

	public boolean Receive(byte[] input) {

		ChattingArea.append("[RECV] " + new String(input) + "\n");
		// input���� ���� byte�� String���� �ٲ㼭 �����ش�.
		return true;

	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) { // ���� ���̾� ����
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) { // ���� ���̾� ����
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
		// nUpperLayerCount++;
	}

	@Override
	public String GetLayerName() { // ���̾� �̸� ��ȯ
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() { // ���� ���̾� ��ȯ
		// TODO Auto-generated method stub
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) { // ���� ���̾� ��ȯ
		// TODO Auto-generated method stub
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) { // �Ű������� ���� ���̾ �������̾�� �����ϰ�, �� ���̾��� ������ �Լ��� ȣ���� ��ü�� ����
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);

	}

	private static String asString(final byte[] mac) {
		// 6����Ʈ ������ mac�� ���ڿ��� �����ϴ� �Լ�
		final StringBuilder buf = new StringBuilder();
		for (byte b : mac) {
			if (buf.length() != 0) {
				buf.append("-");
			}
			if (b >= 0 && b < 16) {
				buf.append('0');// 16�̸��Ͻ� ���ڸ��� ���
			}
			buf.append(Integer.toHexString((b < 0) ? b + 256 : b).toUpperCase());

		}
		return buf.toString();
	}
	
	
	
	

}
