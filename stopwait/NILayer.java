package stopwait;

import java.nio.ByteBuffer;
import java.util.*;

import org.jnetpcap.*;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

public class NILayer implements BaseLayer{
	
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	
	int m_iNumAdapter;
	public Pcap m_AdapterObject;
	public PcapIf device;
	public List<PcapIf> m_pAdapterList;
	StringBuilder errbuf = new StringBuilder();
	
	
	public NILayer(String pName) {
	
		pLayerName = pName;
		
		m_pAdapterList = new ArrayList<PcapIf>();
		m_iNumAdapter = 0;
		SetAdapterList();
		
	}
	
	public void SetAdapterList() {	//��Ʈ��ũ ��� ��� ���� �Լ�
		int r = Pcap.findAllDevs(m_pAdapterList, errbuf);
		
		//���� ��ǻ�Ϳ� �����ϴ� ��� ��Ʈ��ũ ��� ��� ��������
		if(r == Pcap.NOT_OK || m_pAdapterList.isEmpty()) {
			System.err.printf("Can't read list of devices, error is %s", errbuf.toString());
			return;
			//��Ʈ��ũ ��Ͱ� �ϳ��� �������� ������� ���� ó��
		}
	}
	
	public void SetAdapterNumber(int iNum) {//��Ʈ��ũ ��� ���� �Լ�
		m_iNumAdapter = iNum;//���õ� ��Ʈ��ũ ��� �ε����� ���� �ʱ�ȭ
		PacketStartDriver();//��Ŷ ����̹� ���� �Լ�(��Ʈ��ũ ��� ��ü open)
		Receive();//��Ŷ ���� �Լ�
	}
	
	public void PacketStartDriver() {
		int snaplen = 64 * 1024;	//capture all packets, no truncation
		//��Ŷ ĸó ����
		int flags = Pcap.MODE_NON_PROMISCUOUS;//capture all packets
		//��Ŷ ĸó �÷���(PROMISCUOUS;��� ��Ŷ)
		int timeout = 10 * 1000;	//10 seconds in milisecond
		//��Ŷ ĸó �ð�(���� �ð����� ��Ŷ�� ���ŵ��� ���� ��� �������ۿ� �Է�)
		m_AdapterObject = Pcap.openLive(m_pAdapterList.get(m_iNumAdapter).getName(),snaplen, flags, timeout, errbuf);
		//���õ� ��Ʈ��ũ ��� �� ������ �ɼǿ� ������ pcap �۵� ����
	}
	
	public boolean Receive() {
		Receive_Thread thread = new Receive_Thread(m_AdapterObject,this.GetUpperLayer(0));
		//��Ŷ ���Ž� ��Ŷ ó���� ���� runnable Ŭ���� ����
		Thread obj = new Thread(thread);	//Thread ����
		obj.start();	//Thread ����
		
		return false;
	}
	
	public boolean Send(byte[] input, int length) {
		ByteBuffer buf = ByteBuffer.wrap(input);//�������̾�κ��� ���޹��� �����͸� ����Ʈ ���ۿ� ����
		if(m_AdapterObject.sendPacket(buf) != Pcap.OK) {//��Ʈ��ũ ����� sendPacket()�Լ������� ������ ����
			System.err.println(m_AdapterObject.getErr());//��Ŷ ������ ������ ��� �����޽��� ��� �� false ��ȯ
			return false;
		}
		return true;	//��Ŷ������ ������ ��� true ��ȯ
	}
	
	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		// TODO Auto-generated method stub
		if(p_UnderLayer == null)
			return null;
		
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		// TODO Auto-generated method stub
		if(nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0) 
			return null;
			
		
		return p_aUpperLayer.get(nindex);
	}
	
	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		// TODO Auto-generated method stub
		if(pUnderLayer == null) 
			return;
		
		p_UnderLayer = pUnderLayer;
		
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		// TODO Auto-generated method stub
		if(pUpperLayer == null)
			return;
		
		this.p_aUpperLayer.add(nUpperLayerCount++ , pUpperLayer);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		// TODO Auto-generated method stub
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
		
	}
	
	class Receive_Thread implements Runnable{
		byte[] data;
		Pcap AdapterObject;
		BaseLayer UpperLayer;
		public Receive_Thread(Pcap m_AdapterObject, BaseLayer m_UpperLayer) {
			
			AdapterObject = m_AdapterObject;
			UpperLayer = m_UpperLayer;
		}//Pcap ó���� �ʿ��� ��Ʈ��ũ ��� �� ���� ���̾� ��ü �ʱ�ȭ
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(true) {
				PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {
					//��Ŷ ������ ���� ���̺귯���Լ�(PacapPacketHandler)
					public void nextPacket(PcapPacket packet, String user) {
						data = packet.getByteArray(0, packet.size());
						//���ŵ� ��Ŷ�� ������(����Ʈ �迭)�� ��Ŷ ũ�⸦ �˾Ƴ�
						UpperLayer.Receive(data);
						//���ŵ� �����͸� ���� ���̾�� ����
					}
				};
				
				AdapterObject.loop(10000, jpacketHandler, "");
				//��Ʈ��ũ ��Ϳ��� PcapPacketHandler�� ���� �ݺ�
			}
		}
	}

}
