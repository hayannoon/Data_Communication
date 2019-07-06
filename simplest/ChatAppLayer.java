package stopwait;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {	//Application Layer

	public int nUpperLayerCount = 0;	//upperLayer�� 0 �ʱ�ȭ
	public String pLayerName = null;	//Layer�̸� ����
	public BaseLayer p_UnderLayer = null; //UnderLayer ����
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	//UpperLayer ����Ʈ ����
	
	private class _CHAT_APP {
				
	byte[] capp_totlen ; //�ѱ������� ���� (�̹� ���������� �Ⱦ���)
	byte capp_type;  	//Ÿ������ ����(�̹����������� �Ⱦ���)
	byte capp_unused;  
	byte[] capp_data;	//�����͸� ������ ����
	
	public _CHAT_APP () {	//������
		this.capp_totlen = new byte[2];		//2����Ʈ(��������)
		this.capp_type = 0x00;				//1����Ʈ(Ÿ������)
		this.capp_unused = 0x00;			//1����Ʈ
		this.capp_data = null;				//maximum 1456����Ʈ
		}
	}	//����� 4����Ʈ���� �� �� �ִ�.
	
	
	_CHAT_APP m_sHeader = new _CHAT_APP();
	//��� ������ ���� ����
	
	public ChatAppLayer(String pName) {		//������
		// TODO Auto-generated constructor stub
		pLayerName = pName;		//�Ű������� ���� String�� �̸����� ����
		ResetHeader();
	}
	
	public void ResetHeader() {		//����� �ʱ�ȭ�Ѵ�.
		for (int i = 0; i < 2; i++) {
			m_sHeader.capp_totlen[i] = (byte) 0x00;
		}	//�ѱ��� 0���� ����
		m_sHeader.capp_data = null;//�����Ͱ� null����
	}
	

	public byte[] RemoveCappHeader(byte[] input, int length) {	//����� �����ش�.
		
		byte[] buf = new byte[length-4];	//����� 4����Ʈ�� ������ ũ�⸸ŭ�� �迭 ����
		
		for(int i = 0 ; i < length-4 ; i++)	//ó�� 4����Ʈ �����ϰ� �޺κи� ����.
			buf[i] = input[i+4];
		
		return buf;
	}
	
	public synchronized boolean Receive(byte[] input) {	//���� �޾Ƶ��̴� �Լ�
		byte[] data;	//����Ʈ �迭 �������� ����
		
		data = RemoveCappHeader(input, input.length);	//��� �����ϰ�
		this.GetUpperLayer(0).Receive(data);		//receive 
		// �ּҼ���
		return true;
	}
	
	public boolean Send(byte[] input, int length) {		//����ٿ��� ������.
		byte[] buf = new byte[length+4];
		buf[0] = m_sHeader.capp_totlen[0];
		buf[1] = m_sHeader.capp_totlen[1];
		buf[2] = m_sHeader.capp_type;
		buf[3] = m_sHeader.capp_unused;
		
		for(int i = 0 ; i < length ; i++) {
			buf[i+4] = input[i];
		}
		
		this.GetUnderLayer().Send(buf, length + 4);
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
	
}
