package stopwait;

import java.util.ArrayList;

public class EthernetLayer implements BaseLayer {

	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public _ETHERNET_Frame frame = new _ETHERNET_Frame();

	private class _ETHERNET_ADDR { // 이더넷 주소 클래스(6바이트)
		private byte[] addr = new byte[6];

		public _ETHERNET_ADDR() {// 생성자(전부다 0으로 초기화)
			for (int i = 0; i < 6; i++) {
				this.addr[i] = (byte) 0x00;
			}
		}
	}

	private class _ETHERNET_Frame {
		_ETHERNET_ADDR enet_dstaddr; // 이더넷 도착지 주소
		_ETHERNET_ADDR enet_srcaddr; // 이더넷 출발지 주소
		byte[] enet_type;
		byte[] enet_data;

		public _ETHERNET_Frame() { // 생성자
			this.enet_dstaddr = new _ETHERNET_ADDR(); // 도착지 주소 6바이트
			this.enet_srcaddr = new _ETHERNET_ADDR(); // 출발지 주소 6바이트
			this.enet_type = new byte[2]; // 타입 정보 2바이트
			this.enet_data = null; // 데이터
		}// 이더넷 헤더 : 14 바이트
			// 타입정보가 0x01 : Data를 보낸다는 의미
			// 타입정보가 0x02 : Ack를 보낸다는 의미
	}

	_ETHERNET_Frame m_eHeader = new _ETHERNET_Frame();

	public void SetHeader(byte[] src, byte[] dst) {
		System.arraycopy(src, 0, m_eHeader.enet_srcaddr.addr, 0, src.length);
		System.arraycopy(dst, 0, m_eHeader.enet_dstaddr.addr, 0, dst.length);
	}

	public void ResetHeader() { // 헤더를 초기화한다.
		for (int i = 0; i < 6; i++) {
			m_eHeader.enet_dstaddr.addr[i] = (byte) 0x00;
			m_eHeader.enet_srcaddr.addr[i] = (byte) 0x00;
		} // 주소값 0으로 초기화
		m_eHeader.enet_data = null;// 데이터값 null설정
	}

	public byte[] RemoveCappHeader(byte[] input, int length) { // 헤더를 없애준다.

		byte[] buf = new byte[length - 14]; // 헤더의 14바이트를 제외한 크기만큼의 배열 생성
		for (int i = 0; i < length - 14; i++) // 처음 14바이트 제외하고 뒷부분만 복사.
			buf[i] = input[i + 14];
		return buf;
	}

	public boolean Send(byte[] input, int length) { // 헤더붙여서 보낸다.
		byte[] buf = new byte[length + 14];

		System.arraycopy(m_eHeader.enet_dstaddr.addr, 0, buf, 0, 6);// 앞6바이트 도착주소 저장
		System.arraycopy(m_eHeader.enet_srcaddr.addr, 0, buf, 6, 6);// 그뒤에 출발주소 저장
		System.arraycopy(m_eHeader.enet_type, 0, buf, 12, 2);// 타입정보 저장
		System.arraycopy(input, 0, buf, 14, length); // data값 저장
		buf[12] = 0x00;
		buf[13] = 1; // 여기서 Send는 데이터를 보내는거로한다.
		this.GetUnderLayer().Send(buf, length + 14);
		return true;

	}

	public boolean ackSend(byte[] input, int length) {
		byte[] buf = new byte[length + 14];

		System.arraycopy(m_eHeader.enet_dstaddr.addr, 0, buf, 0, 6);// 앞6바이트 도착주소 저장
		System.arraycopy(m_eHeader.enet_srcaddr.addr, 0, buf, 6, 6);// 그뒤에 출발주소 저장
		System.arraycopy(m_eHeader.enet_type, 0, buf, 12, 2);// 타입정보 저장
		System.arraycopy(input, 0, buf, 14, length); // data값 저장
		buf[14] = 'A';
		buf[15] = 'C'; // ACK를 wireshark에서 확인하기위해 필요없는자리에 ACK 문자 저장
		buf[16] = 'K';
		buf[12] = 0x00;
		buf[13] = 0; // 타입값으로 Ack라는걸 알려준다.
		this.GetUnderLayer().Send(buf, length + 14); // 다만든 ACK프레임 전송
		return true;
	}

	public synchronized boolean Receive(byte[] input) { // 정보 받아들이는 함수
		byte[] data;

		if ((input[0] == 0xff) && (input[1] == 0xff) && (input[2] == 0xff) && (input[3] == 0xff) && (input[4] == 0xff)
				&& (input[5] == 0xff)) {
			// broadcasting을 목적지로한 패킷이 도착했을떄 받아준다.
			data = RemoveCappHeader(input, input.length);
			this.GetUpperLayer(0).Receive(data); // receive 실행
			// 주소설정
			return true;
		}

		for (int i = 0; i < 6; i++) {
			if (input[i] != m_eHeader.enet_srcaddr.addr[i]) {
				return false; // 나의 주소와 들어온 패킷의 도착지가 일치하지않으면 false
			}
			if (input[i + 6] != m_eHeader.enet_dstaddr.addr[i]) {
				return false; // 나의 도착지와 들어온 패킷의 출발지가 일치하지 않으면 false
			}
		} // 주소값 검사하고 다르면 false

		// 여기까지 통과했다면 정상적인 주소로 들어온것이다.
		if (input[13] == 0) {
			// 만약 들어온 input값의 타입이 Ack라면
			((ChatAppLayer) this.GetUpperLayer(0)).Sendfragment(); // Sendfragment 호출한다.
			System.out.println("ACK"); // 디버깅용
		} else if (input[13] == 1) {
			// 들어온 input이 Data라면 헤더 떼고, 쳇앱으로 올리고 ACK보낸다.
			data = RemoveCappHeader(input, input.length); // 헤더 제거하고
			this.GetUpperLayer(0).Receive(data); // receive 실행하고
			this.ackSend(input, input.length); // Ack 보낸다.
		}
		return true;
	}

	public EthernetLayer(String pName) {
		this.pLayerName = pName;
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
