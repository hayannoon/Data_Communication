package chat_file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import chat_file.ChatAppLayer.Receive_Thread;

public class FileAppLayer implements BaseLayer {

	public int nUpperLayerCount = 0; // upperLayer�� 0 �ʱ�ȭ
	public String pLayerName = null; // Layer�̸� ����
	public BaseLayer p_UnderLayer = null; // UnderLayer ����
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	public class _FAPP_HEADER { // fileapp header Ŭ����
		byte[] fapp_totlen; // ��ü����
		byte[] fapp_type; // Ÿ������
		byte fapp_msg_type; // �޽���Ÿ��
		byte ed;
		byte[] fapp_seq_num; // sequence number
		byte[] fapp_data; // file data

		public _FAPP_HEADER() { // ��� ������
			this.fapp_totlen = new byte[4]; // ��ü���� 4����Ʈ
			this.fapp_type = new byte[2]; // Ÿ������ 2����Ʈ(����ȭ ������ ��´�.)
			this.fapp_msg_type = 0x00; // ������
			this.ed = 0x00; // ������
			this.fapp_seq_num = new byte[4]; // �������ѹ� 4����Ʈ ������ ��%���´��� �˱����� ���(progressbar)
			this.fapp_data = null; // ���ϵ����� �ִ� 1448����Ʈ
		} // ����� �� 12����Ʈ
	}

	private void makeHeader(int row) { //��� �����ϴ� �Լ�
		byte[] header = new byte[12];
		System.arraycopy(m_sHeader.fapp_totlen, 0, header, 0, 4);// ����
		System.arraycopy(m_sHeader.fapp_type, 0, header, 4, 2);// fappŸ��
		header[6] = m_sHeader.fapp_msg_type;

		System.arraycopy(m_sHeader.fapp_seq_num, 0, header, 8, 4);
		System.arraycopy(header, 0, send_Buffer[row], 0, 12);
	} //row�� ���ڷ� ������ �ش� �࿡ ��������� ����ȴ�.

	_FAPP_HEADER m_sHeader = new _FAPP_HEADER(); // ��� ����
	byte[] file_To_Bytes;
	byte[][] send_Buffer;
	byte[] receive_Buffer;
	String receiveFileName;
	int receive_Rotation = 0;
	int receive_Length = 0;

	public FileAppLayer(String pName) { // ������
		// TODO Auto-generated constructor stub
		pLayerName = pName; // �Ű������� ���� String�� �̸����� ����
		// ResetHeader();
	}

	public byte[] RemoveCappHeader(byte[] input, int length) { // ����� �����ش�.

		byte[] buf = new byte[length - 12]; // ����� 12����Ʈ�� ������ ũ�⸸ŭ�� �迭 ����

		for (int i = 0; i < length - 12; i++) // ó�� 12����Ʈ �����ϰ� �޺κи� ����.
			buf[i] = input[i + 12];

		return buf;
	}

	public class Send_Thread implements Runnable {
		File file;

		public Send_Thread(File file) {
			this.file = file; // ������ ������ �Ű������� �޾ƿ´�.
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			realSend(file);
		}
	}
	
	public boolean Send(String filePath) {

		File file = new File(filePath); // ������ ���� ����
		Send_Thread sendThread = new Send_Thread(file); // ���������
		Thread obj = new Thread(sendThread);
		obj.start();
		return false;
	}

	@SuppressWarnings("resource")
	public byte[] getBytesFromFile(File file) throws IOException { // ������ ����Ʈ�� �ٲٴ� �Լ�
		InputStream is = new FileInputStream(file);
		long length = file.length();

		byte[] bytes = new byte[(int) length]; //����-> ����Ʈ ���� ������ ����Ʈ �迭 ����

		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file" + file.getName());
		}

		is.close();
		return bytes; //������� ����Ʈ�迭 ��ȯ
	}

	public boolean realSend(File file) {

		long length; // ���� ����
		int rotation = 0; // �ݺ�Ƚ��(1448����Ʈ¥���� ����߶���ϴ°�)

		// length = file.length(); // ���� ���̸� ����Ʈ ������ ��ȯ
		length = file.length();
		if (length % 1448 == 0)
			rotation = (((int) length) / 1448) + 1; // ù�κ�,�������κб���
		else
			rotation = ((int) length / 1448) + 2; //// ù�κ�,�������κб��� �ݺ�Ƚ�� ����
		try { // ������ ����Ʈ�� �ٲ۰� ����
			file_To_Bytes = getBytesFromFile(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		send_Buffer = new byte[rotation][1460];
		send_Buffer[0] = new byte[12 + file.getName().getBytes().length]; // ù�κ��� ���,�����̸��� ������.
		// send_Buffer[rotation-1] = new byte[12 + ((int)length/1448)+1];
		send_Buffer[rotation - 1] = new byte[12 + ((int) length % 1448)];
		// �������κ��� �̷��� ����

		// ����� ü�� ����
		byte[] len = intToByte4((int) length);
		m_sHeader.fapp_totlen = len; // �������� ����

		for (int i = 0; i < rotation; i++) {
			if (i == 0) {
				m_sHeader.fapp_type = intToByte2(0);
				// ù�κ��� Ÿ�� 0x00
				m_sHeader.fapp_msg_type = 0x00;
				m_sHeader.fapp_seq_num = intToByte4(i - 1);
				// sequencenumber ����
				makeHeader(i);
				// ��� ����
				byte[] tempName = file.getName().getBytes(); // �����̸�����Ʈ������
				System.arraycopy(tempName, 0, send_Buffer[i], 12, tempName.length);
			}

			else if (i == rotation - 1) {
				m_sHeader.fapp_type = intToByte2(2);
				// �������κ��� Ÿ�� 0x02
				m_sHeader.fapp_msg_type = 0x01;
				m_sHeader.fapp_seq_num = intToByte4(i - 1);
				// sequencenumber ����
				makeHeader(i);
				System.arraycopy(file_To_Bytes, (i - 1) * 1448, send_Buffer[i], 12, (int) length % 1448);

			} else {
				m_sHeader.fapp_type = intToByte2(1);// �߰��κ��� Ÿ��0x01
				m_sHeader.fapp_msg_type = 0x01;
				m_sHeader.fapp_seq_num = intToByte4(i - 1);
				// sequencenumber ����
				makeHeader(i);
				// ��� �� �����ѵ�
				System.arraycopy(file_To_Bytes, (i - 1) * 1448, send_Buffer[i], 12, 1448);
				// ���ϵ� ����
			}
		}
		// ------------------------������� ���� ����ȭ ��!--------------------------
		for (int i = 0; i < rotation; i++) {
			this.GetUnderLayer().Send(send_Buffer[i], send_Buffer[i].length);
			int forProgressBar = (int) ((((double) i) / (rotation - 1)) * 100);// �����鼭 ���α׷����� ������Ʈ
			((ChatFileDlg) p_aUpperLayer.get(0)).progressBar.setValue(forProgressBar);
			
			//**********************���α׷��� ��Ȯ��/�ӵ��� ���⼭ �����ȴ�.*************************
			if ((i % 5 == 0)) {	
				try {
					Thread.sleep(1); //������ ��� ���
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//**********************���α׷��� ��Ȯ��/�ӵ��� ���⼭ �����ȴ�.*************************
		}
		return true;
	}

	

	public class Receive_Thread implements Runnable {

		byte[] data;

		public Receive_Thread(byte[] data) { // ������
			this.data = data;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Realreceive(data);//realReceive ȣ���Ѵ�.
		}

	}

	public synchronized boolean Receive(byte[] input) { //�������� ȣ���ϴ� Receive�Լ�
		Receive_Thread receiveThread = new Receive_Thread(input);
		Thread obj = new Thread(receiveThread);//���������
		obj.start(); //run�� �����Ѵ�.
		return false;

	}

	public synchronized boolean Realreceive(byte[] input) {// ���������� Receive�� �����Ǵ°�
		if ((input[4] == 0x00) && (input[6] == 0x00)) { // ù��° �����ΰ��-�����̸��� ����ִ�.
			byte[] tempForLength = new byte[4]; // ���̸� ���Ϸ��� �ӽ÷� ���� ����Ʈ�迭 ����
			System.arraycopy(input, 0, tempForLength, 0, 4);// �������� ����
			receive_Length = byteToInt(tempForLength); // ��ü���� ����
			if (receive_Length % 1448 == 0)
				receive_Rotation = ((receive_Length) / 1448); // ù�κ�,�������κб���
			else
				receive_Rotation = (receive_Length / 1448) + 1; //// ù�κ�,�������κб��� �ݺ�Ƚ�� ����

			byte[] tempForName = RemoveCappHeader(input, input.length); // ��� ����
			receiveFileName = new String(tempForName); // �����̸� ����
			receive_Buffer = new byte[receive_Length];
		} else if ((input[4] == 0x01) && (input[6] == 0x01)) { // �������̰�, ������������ �ƴѰ��
			byte[] tempForSequenceNumber = new byte[4];
			System.arraycopy(input, 8, tempForSequenceNumber, 0, 4); // sequence Number ����
			int sNumber = byteToInt(tempForSequenceNumber); // sequenceNubmer����
			byte[] data = RemoveCappHeader(input, input.length); // ��� ����
			System.arraycopy(data, 0, receive_Buffer, 1448 * sNumber, 1448); // sequenceNumber�� �°� ������ ����
			int forProgressBar = (int) ((((double) sNumber) / (receive_Rotation - 1)) * 100);
			((ChatFileDlg) p_aUpperLayer.get(0)).progressBar.setValue(forProgressBar);// progressbar ����

		} else { // Ÿ���� 0x02�ΰ�� -> ������ �����Ͱ� ���� ���
			byte[] tempForSequenceNumber = new byte[4];
			System.arraycopy(input, 8, tempForSequenceNumber, 0, 4); // sequence Number ����
			int sNumber = byteToInt(tempForSequenceNumber); // sequenceNubmer����
			byte[] data = RemoveCappHeader(input, input.length); // ��� ����
			
			
			System.arraycopy(data, 0, receive_Buffer, 1448 * sNumber, data.length); // sequenceNumber�� �°� ������ ����
			int forProgressBar = (int) ((((double) sNumber) / (receive_Rotation - 1)) * 100);
			((ChatFileDlg) p_aUpperLayer.get(0)).progressBar.setValue(forProgressBar);// progressbar ����
			// �̶��� receive_Buffer�� ���� ���ִ�. ���� ���Ϸ� ���� �ø��⸸�ϸ� �ȴ�.
			
			try { //Ȥ�� ������ �������� ������ �� �տ��͵��� �ȿ�����츦 ����ؼ� ��ٷ��ش�.
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			((ChatFileDlg) this.GetUpperLayer(0)).ReceiveFile(receiveFileName, receive_Buffer);
		}

		return false;
	}

	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		return pLayerName;
	} 
	
	
	@Override
	public BaseLayer GetUnderLayer() {
		// TODO Auto-generated method stub
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		// TODO Auto-generated method stub
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
		// nUpperLayerCount++;
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		// TODO Auto-generated method stub
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}

	byte[] intToByte2(int value) { // int�� (10����) �޾Ƽ� ����Ʈ�������� ��ȯ(2����Ʈ)
		byte[] temp = new byte[2];
		temp[1] = (byte) (value >> 8);
		temp[0] = (byte) value;

		return temp;
	}

	byte[] intToByte4(int value) {// int�� (10����) �޾Ƽ� ����Ʈ�������� ��ȯ(4����Ʈ)
		byte[] temp = new byte[4];

		temp[0] |= (byte) ((value & 0xFF000000) >> 24);
		temp[1] |= (byte) ((value & 0xFF0000) >> 16);
		temp[2] |= (byte) ((value & 0xFF00) >> 8);
		temp[3] |= (byte) (value & 0xFF);

		return temp;
	}

	public int byteToInt(byte[] arr) { // byte�� int�� �ٲٴ� �Լ�
		return (arr[0] & 0xff) << 24 | (arr[1] & 0xff) << 16 | (arr[2] & 0xff) << 8 | (arr[3] & 0xff);
	}

	public File makeFileFromBytes(String fileName, byte[] data) { // �����͸� ���Ϸ� ������ش�.
		File file = new File(fileName);
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
