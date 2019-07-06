package ipc;
import java.util.ArrayList;

interface BaseLayer {
	public final int m_nUpperLayerCount = 0;		//UpperLayer의 개수 0으로 초기화
	public final String m_pLayerName = null;		//LayerName 초기화		
	public final BaseLayer mp_UnderLayer = null;	//underLayer 초기화
	public final ArrayList<BaseLayer> mp_aUpperLayer = new ArrayList<BaseLayer>();

	public String GetLayerName();		//Layer의 이름을 반환한다.

	public BaseLayer GetUnderLayer();	//하위계층의 이름을 반환한다.

	public BaseLayer GetUpperLayer(int nindex);	//index번째 상위계층을 반환한다.

	public void SetUnderLayer(BaseLayer pUnderLayer);	//underLayer Setter
	
	public void SetUpperLayer(BaseLayer pUpperLayer);	//upperLayer Setter

	public default void SetUnderUpperLayer(BaseLayer pUULayer) {
		//매개변수로 들어온 레이어를 upperLayer로 설정하고, 그 레이어의 underLayer로 함수를 호출한 객체를 설정한다. 
	}

	public void SetUpperUnderLayer(BaseLayer pUULayer);	
	//매개변수로 들어온 레이어를 UnderLayer로 설정하고, 그 레이어의 upperLayer로 함수를 호출한 객체를 설정한다.

	public default boolean Send(byte[] input, int length) {
		return false;
	}	//byte배열정보와, 그 길이를 매개변수로 받아서 전달해준다.

	public default boolean Send(String filename) {
		return false;	//filename을 매개변수로 받아서 전달해준다.
	}

	public default boolean Receive(byte[] input) {
		return false;
	}

	public default boolean Receive() {
		return false;
	}

}
