package chat_file;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer { // Application Layer

	public int nUpperLayerCount = 0; // upperLayer수 0 초기화
	public String pLayerName = null; // Layer이름 변수
	public BaseLayer p_UnderLayer = null; // UnderLayer 변수
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	// UpperLayer 리스트 생성
	public int count;
	byte[] receiveBuffer; // 길이정보를 보고 생성하게될 버퍼
	byte[][] buf; // Send시, 단편화된 데이터를 보관할 버퍼
	int buflength; // 버퍼 길이를 저장할 변수
	int bufcount; // receive시 반복횟수를 저장할 변수
	int rotate; // 반복 횟수를 저장하기위한 변수
	int sendCount; // send시 반복횟수를 저장할 변수

	private class _CHAT_APP {

		byte[] capp_totlen; // 총길이정보 저장
		byte capp_type; // 타입정보 저장
		byte capp_unused;
		byte[] capp_data; // 데이터를 저장할 변수

		public _CHAT_APP() { // 생성자
			this.capp_totlen = new byte[2]; // 2바이트(길이정보)
			this.capp_type = 0x00; // 1바이트(타입정보) - 0x00:단편화되지않음 / 0x01:단편화첫부분 /0x02:단편화 중간 / 0x03:단편화 마지막 
			this.capp_unused = 0b01010110; // 1바이트
			this.capp_data = null; // maximum 1456바이트 사용자가 입력한 문자열을 저장한다.
		}
	} // 헤더는 4바이트임을 알 수 있다.

	_CHAT_APP m_sHeader = new _CHAT_APP();
	// 헤더 저장할 변수 생성

	public ChatAppLayer(String pName) { // 생성자
		// TODO Auto-generated constructor stub
		pLayerName = pName; // 매개변수로 들어온 String을 이름으로 설정
		ResetHeader();
	}

	public void ResetHeader() { // 헤더를 초기화한다.
		for (int i = 0; i < 2; i++) {
			m_sHeader.capp_totlen[i] = (byte) 0x00;
		} // 총길이 0으로 설정
		m_sHeader.capp_data = null;// 데이터값 null설정
	}

	public byte[] RemoveCappHeader(byte[] input, int length) { // 헤더를 없애준다.

		byte[] buf = new byte[length - 4]; // 헤더의 4바이트를 제외한 크기만큼의 배열 생성

		for (int i = 0; i < length - 4; i++) // 처음 4바이트 제외하고 뒷부분만 복사.
			buf[i] = input[i + 4];

		return buf;
	}
	
	

	public synchronized boolean RealReceive(byte[] input) { // 정보 받아들이는 함수
		byte[] data; // 바이트 배열 참조변수 선언
		if (input[2] == 0x00) {
			// 단편화가 없는 데이터라면
			data = RemoveCappHeader(input, input.length); // 헤더 제거하고
			((ChatFileDlg)this.GetUpperLayer(0)).ReceiveChat(data); // receive
			return true;
		}

		else { // 단편화가 있다면
			if (input[2] == 0x01) {
				// 첫번째 데이터라면
				count = 0;
				int len = ( input[0] * 16 )+ input[1]; // 길이 계산
				this.receiveBuffer = new byte[len]; // 버퍼생성
				data = RemoveCappHeader(input, input.length); // 헤더 제거하고
				System.arraycopy(data, 0, receiveBuffer, count, 1456);// 버퍼에 복사
				count += 1456; // 다음index 설정

			} // 첫번째 버퍼에 저장
			else if (input[2] == 0x03) {
				// 마지막 단편화 조각이라면
				int len = input[0] * 16 + input[1]; // 길이 계산
				data = RemoveCappHeader(input, input.length); // 헤더 제거하고

				if (len % 1456 == 0)
					System.arraycopy(data, 0, receiveBuffer, count, 1456);
				else
					System.arraycopy(data, 0, receiveBuffer, count, len % 1456); // 버퍼에 복사

				((ChatFileDlg)this.GetUpperLayer(0)).ReceiveChat(receiveBuffer);
				// 데이터가 모두 모였으니 상위계층으로 드디어 전송!
			} else {
				// 중간단편화 조각이라면
				data = RemoveCappHeader(input, input.length); // 헤더 제거하고
				System.arraycopy(data, 0, receiveBuffer, count, 1456); // 버퍼에 복사
				count += 1456; // 다음 index 설정
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
		//보낼때 생성되는 쓰레드
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
			//run을 실행하면 내가 구현한 진짜로 Send를 실행하는 함수를 실행시킨다.
		}
		
	}
	
	public boolean Send(byte[] input, int length) { //상위에서 호출하는 Send함수
		Send_Thread sendThread = new Send_Thread(input,length);
		Thread obj = new Thread(sendThread); //쓰레드 생성
		obj.start(); //쓰레드 run 실행
		return false;
	}
	
	public boolean RealSend(byte[] input, int length) { 
		//실제로 Send가 구현되어있는 함수. 쓰레드가 실행되면 결과적으로 이 함수를 호출한다.

		m_sHeader.capp_totlen[1] = (byte) length;
		m_sHeader.capp_totlen[0] = (byte) (length >> 8);
		// 길이정보 헤더에 저장
		buflength = length; // 버퍼길이저장
		bufcount = 0;
		sendCount = 0;
		if (length % 1456 == 0)
			rotate = length / 1456;
		else
			rotate = (length / 1456) + 1;
		// 반복 횟수 설정
		buf = new byte[rotate][1460]; // 단편화조각 객체생성
		if (length % 1456 != 0)
			buf[rotate - 1] = new byte[length % 1456 + 4];
		// 마지막버퍼조각은 이렇게 설정
		for (int i = 0; i < rotate; i++) {
			if (i == rotate - 1) {
				// 마지막조각인경우
				if (length % 1456 == 0) {
					System.arraycopy(input, i * 1456, buf[i], 4, 1456);
				} else {
					System.arraycopy(input, i * 1456, buf[i], 4, length % 1456);
				}
			} else { // 마지막 아니라면 10칸씩 복사
				System.arraycopy(input, i * 1456, buf[i], 4, 1456);
			}
			buf[i][0] = m_sHeader.capp_totlen[0];
			buf[i][1] = m_sHeader.capp_totlen[1]; //버퍼의 헤더에 길이 저장
			buf[i][3] = m_sHeader.capp_unused;
		}
		
		// --------------------여기까지 단편화 끝!--------------------------------

		if (buflength <= 1456) {
			// 10보다 작으면 단편화하지않는다.
			m_sHeader.capp_type = 0x00; // 단편화하지않음을 표시
			buf[0][2] = m_sHeader.capp_type;
			this.GetUnderLayer().Send(buf[0], buflength + 4);
			// 하위로 바로 전송하고
			sendCount++;
			return true;
		} else
			for(int i = 0 ; i < rotate ; i++) { //rotation만큼 Sendfragment호출해서 전송
			this.Sendfragment();
			//이번엔 ACK를 받지 않기때문에 단순히 반복문으로 보낸다.
			}
		// 단편화가 된 데이터라면 Sendfragment호출해서 거기서 전송하도록 한다.
		return true;
	}

	public boolean Sendfragment() {
		// 단편화된 데이터를 순서대로 보내기위한 함수

		if (sendCount == rotate) {
			// 만약 ACK를 받았어도, 마지막데이터에대한 ACK라면 전송하지않아야하므로, 빠져나온다.
			return true;
		}
		if (rotate == 1)
			return true; // 단편화없는데이터의 ACK받으면 더이상전송하지않는다!
		// 혹시몰라서 한번더 예외처리했습니다.

		else { // 단편화가 필요한 경우

			if (sendCount == 0)
				m_sHeader.capp_type = 0x01; // 첫번째 프레임 타입은 0x01
			else if (sendCount == rotate - 1)
				m_sHeader.capp_type = 0x03; // 마지막프레임 타입은 0x03;
			else
				m_sHeader.capp_type = 0x02; // 나머지는 중간이므로 0x02
			buf[sendCount][2] = m_sHeader.capp_type; // 버퍼에 타입정보 입력

			if (buf[sendCount][2] == 0x03) {
				// 마지막 단편화 조각이라면
				if (buflength % 1456 == 0) { // 마지막이 1456칸꽉채운경우
					this.GetUnderLayer().Send(buf[sendCount], 1460);
				} else { // 마지막이 꽉 안채운경우는 길이만큼생성해서보낸다.
					this.GetUnderLayer().Send(buf[sendCount], (buflength % 1456) + 4);
				}
			} else {
				this.GetUnderLayer().Send(buf[sendCount], 1460);
				// 처음과,중간은 모두 길이가 1456+4이므로 이렇게 보낸다.
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

	byte[] intToByte2(int value) {			//int형 (10진수) 받아서 바이트형식으로 변환(2바이트)
		byte[] temp = new byte[2];
		temp[1] = (byte) (value >> 8);
		temp[0] = (byte) value;

		return temp;
	}

	byte[] intToByte4(int value) {//int형 (10진수) 받아서 바이트형식으로 변환(4바이트)
		byte[] temp = new byte[4];
		
		temp[0] |= (byte) ((value & 0xFF000000) >> 24 );
		temp[1] |= (byte) ((value & 0xFF0000) >> 16 );
		temp[2] |= (byte) ((value & 0xFF00) >> 8 );
		temp[3] |= (byte) (value & 0xFF);
		
		return temp;
	}

	
}
