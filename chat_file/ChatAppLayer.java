package chat_file;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer { // Application Layer

	public int nUpperLayerCount = 0; // upperLayer�� 0 �ʱ�ȭ
	public String pLayerName = null; // Layer�̸� ����
	public BaseLayer p_UnderLayer = null; // UnderLayer ����
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	// UpperLayer ����Ʈ ����
	public int count;
	byte[] receiveBuffer; // ���������� ���� �����ϰԵ� ����
	byte[][] buf; // Send��, ����ȭ�� �����͸� ������ ����
	int buflength; // ���� ���̸� ������ ����
	int bufcount; // receive�� �ݺ�Ƚ���� ������ ����
	int rotate; // �ݺ� Ƚ���� �����ϱ����� ����
	int sendCount; // send�� �ݺ�Ƚ���� ������ ����

	private class _CHAT_APP {

		byte[] capp_totlen; // �ѱ������� ����
		byte capp_type; // Ÿ������ ����
		byte capp_unused;
		byte[] capp_data; // �����͸� ������ ����

		public _CHAT_APP() { // ������
			this.capp_totlen = new byte[2]; // 2����Ʈ(��������)
			this.capp_type = 0x00; // 1����Ʈ(Ÿ������) - 0x00:����ȭ�������� / 0x01:����ȭù�κ� /0x02:����ȭ �߰� / 0x03:����ȭ ������ 
			this.capp_unused = 0b01010110; // 1����Ʈ
			this.capp_data = null; // maximum 1456����Ʈ ����ڰ� �Է��� ���ڿ��� �����Ѵ�.
		}
	} // ����� 4����Ʈ���� �� �� �ִ�.

	_CHAT_APP m_sHeader = new _CHAT_APP();
	// ��� ������ ���� ����

	public ChatAppLayer(String pName) { // ������
		// TODO Auto-generated constructor stub
		pLayerName = pName; // �Ű������� ���� String�� �̸����� ����
		ResetHeader();
	}

	public void ResetHeader() { // ����� �ʱ�ȭ�Ѵ�.
		for (int i = 0; i < 2; i++) {
			m_sHeader.capp_totlen[i] = (byte) 0x00;
		} // �ѱ��� 0���� ����
		m_sHeader.capp_data = null;// �����Ͱ� null����
	}

	public byte[] RemoveCappHeader(byte[] input, int length) { // ����� �����ش�.

		byte[] buf = new byte[length - 4]; // ����� 4����Ʈ�� ������ ũ�⸸ŭ�� �迭 ����

		for (int i = 0; i < length - 4; i++) // ó�� 4����Ʈ �����ϰ� �޺κи� ����.
			buf[i] = input[i + 4];

		return buf;
	}
	
	

	public synchronized boolean RealReceive(byte[] input) { // ���� �޾Ƶ��̴� �Լ�
		byte[] data; // ����Ʈ �迭 �������� ����
		if (input[2] == 0x00) {
			// ����ȭ�� ���� �����Ͷ��
			data = RemoveCappHeader(input, input.length); // ��� �����ϰ�
			((ChatFileDlg)this.GetUpperLayer(0)).ReceiveChat(data); // receive
			return true;
		}

		else { // ����ȭ�� �ִٸ�
			if (input[2] == 0x01) {
				// ù��° �����Ͷ��
				count = 0;
				int len = ( input[0] * 16 )+ input[1]; // ���� ���
				this.receiveBuffer = new byte[len]; // ���ۻ���
				data = RemoveCappHeader(input, input.length); // ��� �����ϰ�
				System.arraycopy(data, 0, receiveBuffer, count, 1456);// ���ۿ� ����
				count += 1456; // ����index ����

			} // ù��° ���ۿ� ����
			else if (input[2] == 0x03) {
				// ������ ����ȭ �����̶��
				int len = input[0] * 16 + input[1]; // ���� ���
				data = RemoveCappHeader(input, input.length); // ��� �����ϰ�

				if (len % 1456 == 0)
					System.arraycopy(data, 0, receiveBuffer, count, 1456);
				else
					System.arraycopy(data, 0, receiveBuffer, count, len % 1456); // ���ۿ� ����

				((ChatFileDlg)this.GetUpperLayer(0)).ReceiveChat(receiveBuffer);
				// �����Ͱ� ��� ������ ������������ ���� ����!
			} else {
				// �߰�����ȭ �����̶��
				data = RemoveCappHeader(input, input.length); // ��� �����ϰ�
				System.arraycopy(data, 0, receiveBuffer, count, 1456); // ���ۿ� ����
				count += 1456; // ���� index ����
			}
		}
		return true;
	}
	
	public class Receive_Thread implements Runnable{
		byte[] data;
		public Receive_Thread(byte[] data) {
			this.data = data;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			RealReceive(data);
		}
		
	}
	
	public synchronized boolean Receive(byte[] input) {
		Receive_Thread receiveThread = new Receive_Thread(input);
		Thread obj = new Thread(receiveThread);
		obj.start();
		return false;
	}

	public class Send_Thread implements Runnable{
		//������ �����Ǵ� ������
		byte[] data;
		int length;
		public Send_Thread(byte[] data, int length){
			this.data = data;
			this.length = length;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			RealSend(data,length);
			//run�� �����ϸ� ���� ������ ��¥�� Send�� �����ϴ� �Լ��� �����Ų��.
		}
		
	}
	
	public boolean Send(byte[] input, int length) { //�������� ȣ���ϴ� Send�Լ�
		Send_Thread sendThread = new Send_Thread(input,length);
		Thread obj = new Thread(sendThread); //������ ����
		obj.start(); //������ run ����
		return false;
	}
	
	public boolean RealSend(byte[] input, int length) { 
		//������ Send�� �����Ǿ��ִ� �Լ�. �����尡 ����Ǹ� ��������� �� �Լ��� ȣ���Ѵ�.

		m_sHeader.capp_totlen[1] = (byte) length;
		m_sHeader.capp_totlen[0] = (byte) (length >> 8);
		// �������� ����� ����
		buflength = length; // ���۱�������
		bufcount = 0;
		sendCount = 0;
		if (length % 1456 == 0)
			rotate = length / 1456;
		else
			rotate = (length / 1456) + 1;
		// �ݺ� Ƚ�� ����
		buf = new byte[rotate][1460]; // ����ȭ���� ��ü����
		if (length % 1456 != 0)
			buf[rotate - 1] = new byte[length % 1456 + 4];
		// ���������������� �̷��� ����
		for (int i = 0; i < rotate; i++) {
			if (i == rotate - 1) {
				// �����������ΰ��
				if (length % 1456 == 0) {
					System.arraycopy(input, i * 1456, buf[i], 4, 1456);
				} else {
					System.arraycopy(input, i * 1456, buf[i], 4, length % 1456);
				}
			} else { // ������ �ƴ϶�� 10ĭ�� ����
				System.arraycopy(input, i * 1456, buf[i], 4, 1456);
			}
			buf[i][0] = m_sHeader.capp_totlen[0];
			buf[i][1] = m_sHeader.capp_totlen[1]; //������ ����� ���� ����
			buf[i][3] = m_sHeader.capp_unused;
		}
		
		// --------------------������� ����ȭ ��!--------------------------------

		if (buflength <= 1456) {
			// 10���� ������ ����ȭ�����ʴ´�.
			m_sHeader.capp_type = 0x00; // ����ȭ���������� ǥ��
			buf[0][2] = m_sHeader.capp_type;
			this.GetUnderLayer().Send(buf[0], buflength + 4);
			// ������ �ٷ� �����ϰ�
			sendCount++;
			return true;
		} else
			for(int i = 0 ; i < rotate ; i++) { //rotation��ŭ Sendfragmentȣ���ؼ� ����
			this.Sendfragment();
			//�̹��� ACK�� ���� �ʱ⶧���� �ܼ��� �ݺ������� ������.
			}
		// ����ȭ�� �� �����Ͷ�� Sendfragmentȣ���ؼ� �ű⼭ �����ϵ��� �Ѵ�.
		return true;
	}

	public boolean Sendfragment() {
		// ����ȭ�� �����͸� ������� ���������� �Լ�

		if (sendCount == rotate) {
			// ���� ACK�� �޾Ҿ, �����������Ϳ����� ACK��� ���������ʾƾ��ϹǷ�, �������´�.
			return true;
		}
		if (rotate == 1)
			return true; // ����ȭ���µ������� ACK������ ���̻����������ʴ´�!
		// Ȥ�ø��� �ѹ��� ����ó���߽��ϴ�.

		else { // ����ȭ�� �ʿ��� ���

			if (sendCount == 0)
				m_sHeader.capp_type = 0x01; // ù��° ������ Ÿ���� 0x01
			else if (sendCount == rotate - 1)
				m_sHeader.capp_type = 0x03; // ������������ Ÿ���� 0x03;
			else
				m_sHeader.capp_type = 0x02; // �������� �߰��̹Ƿ� 0x02
			buf[sendCount][2] = m_sHeader.capp_type; // ���ۿ� Ÿ������ �Է�

			if (buf[sendCount][2] == 0x03) {
				// ������ ����ȭ �����̶��
				if (buflength % 1456 == 0) { // �������� 1456ĭ��ä����
					this.GetUnderLayer().Send(buf[sendCount], 1460);
				} else { // �������� �� ��ä����� ���̸�ŭ�����ؼ�������.
					this.GetUnderLayer().Send(buf[sendCount], (buflength % 1456) + 4);
				}
			} else {
				this.GetUnderLayer().Send(buf[sendCount], 1460);
				// ó����,�߰��� ��� ���̰� 1456+4�̹Ƿ� �̷��� ������.
			}

		}
		sendCount++;
		return true;
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

	byte[] intToByte2(int value) {			//int�� (10����) �޾Ƽ� ����Ʈ�������� ��ȯ(2����Ʈ)
		byte[] temp = new byte[2];
		temp[1] = (byte) (value >> 8);
		temp[0] = (byte) value;

		return temp;
	}

	byte[] intToByte4(int value) {//int�� (10����) �޾Ƽ� ����Ʈ�������� ��ȯ(4����Ʈ)
		byte[] temp = new byte[4];
		
		temp[0] |= (byte) ((value & 0xFF000000) >> 24 );
		temp[1] |= (byte) ((value & 0xFF0000) >> 16 );
		temp[2] |= (byte) ((value & 0xFF00) >> 8 );
		temp[3] |= (byte) (value & 0xFF);
		
		return temp;
	}

	
}
