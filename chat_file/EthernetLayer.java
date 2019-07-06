package chat_file;

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
		System.arraycopy(m_eHeader.enet_srcaddr.addr, 0, buf, 6, 6);// 뒤6바이트 출발주소 저장
		System.arraycopy(m_eHeader.enet_type, 0, buf, 12, 2);// 타입정보 저장
		System.arraycopy(input, 0, buf, 14, length); // data값 저장
		buf[12]=20;
		
	    if(input[3]==0b01010110) 
	    	buf[13] = 80; // chat의 타입은 2080이다.
		else
			buf[13]=90; //파일앱에서 왔으면 2090
			
		this.GetUnderLayer().Send(buf, length + 14);
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
		
		 if (input[12]==20 && input[13]==80) {
			// 들어온 input이 Chat이라면(2080) 헤더 떼고, 쳇앱으로 올린다.
			data = RemoveCappHeader(input, input.length); // 헤더 제거하고
			this.GetUpperLayer(0).Receive(data); // ChatAppLayer receive 실행하고
		} 
		 //들어온 input이 File이라면(2090) 헤더 뗴고, 파일앱으로 올린다.
		 else if(input[12]==20 && input[13]==90) {
			data = RemoveCappHeader(input, input.length); // 헤더 제거하고
			this.GetUpperLayer(1).Receive(data); // FileAppLayer receive 실행한다.
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
