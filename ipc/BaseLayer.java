package ipc;
import java.util.ArrayList;

interface BaseLayer {
	public final int m_nUpperLayerCount = 0;		//UpperLayer�� ���� 0���� �ʱ�ȭ
	public final String m_pLayerName = null;		//LayerName �ʱ�ȭ		
	public final BaseLayer mp_UnderLayer = null;	//underLayer �ʱ�ȭ
	public final ArrayList<BaseLayer> mp_aUpperLayer = new ArrayList<BaseLayer>();

	public String GetLayerName();		//Layer�� �̸��� ��ȯ�Ѵ�.

	public BaseLayer GetUnderLayer();	//���������� �̸��� ��ȯ�Ѵ�.

	public BaseLayer GetUpperLayer(int nindex);	//index��° ���������� ��ȯ�Ѵ�.

	public void SetUnderLayer(BaseLayer pUnderLayer);	//underLayer Setter
	
	public void SetUpperLayer(BaseLayer pUpperLayer);	//upperLayer Setter

	public default void SetUnderUpperLayer(BaseLayer pUULayer) {
		//�Ű������� ���� ���̾ upperLayer�� �����ϰ�, �� ���̾��� underLayer�� �Լ��� ȣ���� ��ü�� �����Ѵ�. 
	}

	public void SetUpperUnderLayer(BaseLayer pUULayer);	
	//�Ű������� ���� ���̾ UnderLayer�� �����ϰ�, �� ���̾��� upperLayer�� �Լ��� ȣ���� ��ü�� �����Ѵ�.

	public default boolean Send(byte[] input, int length) {
		return false;
	}	//byte�迭������, �� ���̸� �Ű������� �޾Ƽ� �������ش�.

	public default boolean Send(String filename) {
		return false;	//filename�� �Ű������� �޾Ƽ� �������ش�.
	}

	public default boolean Receive(byte[] input) {
		return false;
	}

	public default boolean Receive() {
		return false;
	}

}
