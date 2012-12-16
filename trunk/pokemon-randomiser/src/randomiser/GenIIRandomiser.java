package randomiser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GenIIRandomiser extends Randomiser {

	final int GSStartersOffset = 0x1800d2;
	final int CStartersOffset = 0x78c7f;
	
	final int GSWildOffset = 0x2ab35;
	final int CWildOffset = 0x2a5e9;
	
	final int GSTrainersStart = 0x399c9;
	final int GSTrainersEnd = 0x3b684;
	final int CTrainersStart = 0x39a26;
	final int CTrainersEnd = 0x3ba66;
	
	final int GSHeadbuttStart = 0xba47c;
	final int GSHeadbuttEnd = 0xba4ee;
	final int CHeadbuttStart = 0xb82fa;
	final int CHeadbuttEnd = 0xb83e5;
	
	final int GSFishingOffset = 0x92A52;
	final int CFishingOffset = 0x924e3;
	final int FishingLength1 = 0x18C;
	final int FishingLength2 = 0x58;
	
	Map<Byte,Byte> oneToOneMap;
	Map<String,Byte> nameToIndex;
	String[] indexToName;
	
	public GenIIRandomiser(){
		super();
		oneToOneMap = new HashMap<Byte,Byte>();
		nameToIndex = new HashMap<String,Byte>();
		indexToName = new String[252];
		loadNames();
		ArrayList<Byte> tmpindices = new ArrayList<Byte>(251);
		for(int i=0; i<251; i++)
			tmpindices.add(i,(byte)(i+1));
		
		for(int i=1; i<=251; i++){
			int j = rand.nextInt(tmpindices.size());
			byte replacement = tmpindices.get(j);
			tmpindices.remove(j);
			oneToOneMap.put((byte)i, replacement);
		}
		
	}
	
	private void loadNames(){
		indexToName[0] = null;
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("pokeindices2.txt")));
			while(true){
				String line = r.readLine();
				if(line == null)
					break;
				String[] s = line.split("\t");
				int index = Integer.parseInt(s[0], 16);
				indexToName[index] = s[1];
				nameToIndex.put(s[1], (byte)index);
			}
			
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	@Override
	public String[] getNames(){
		return Arrays.copyOfRange(indexToName, 1, indexToName.length);
	}
	
	@Override
	public boolean isCompatibleVersion(version game) {
		switch(game){
			case Gold:
			case Silver:
			case Crystal:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void randomise() {
		if (starters == startersMode.Random){
			int offset = game==version.Crystal ? CStartersOffset : GSStartersOffset;
			setStarter(offset, getReplacement(rom[offset]));
			setStarter(offset+66, getReplacement(rom[offset+66]));
			setStarter(offset+126, getReplacement(rom[offset+126]));
		}
		else if(starters == startersMode.Custom && customStarters.length == 3){
			int offset = game==version.Crystal ? CStartersOffset : GSStartersOffset;
			setStarter(offset, nameToIndex.get(customStarters[0]));
			setStarter(offset+66, nameToIndex.get(customStarters[1]));
			setStarter(offset+126, nameToIndex.get(customStarters[2]));
		}

		if(wild){
			int offset = game==version.Crystal ? CWildOffset : GSWildOffset;
			for(int i=0; i<3; i++){
				while(rom[offset] != (byte)255){
					offset = randomiseLandArea(offset);
				}
				offset++;
				while(rom[offset] != (byte)255){
					offset = randomiseWaterArea(offset);
				}
				offset++;
			}
			int headbuttStart = game==version.Crystal ? CHeadbuttStart : GSHeadbuttStart;
			int headbuttEnd = game==version.Crystal ? CHeadbuttEnd : GSHeadbuttEnd;
			for(offset=headbuttStart; offset<headbuttEnd;){
				offset = randomiseHBList(offset);
			}
			
			offset = game==version.Crystal ? CFishingOffset : GSFishingOffset;
			randomiseFishingAreas(offset);
		}
		
		if(trainers){
			int start = game==version.Crystal ? CTrainersStart : GSTrainersStart;
			int end = game==version.Crystal ? CTrainersEnd : GSTrainersEnd;
			
			for(int offset = start; offset < end;){
				if(rom[offset] == (byte)80){
					offset = randomiseTrainer(offset);
				}
				else{
					// search for start of next trainer
					offset += 1;
				}
			}
		}
	}
	
	private byte getReplacement(byte pkmn){
		switch(mode){
			case Random:
				return (byte) (rand.nextInt(251)+1);
			case OneToOne:
				Byte replacement =  oneToOneMap.get(pkmn);
				if(replacement != null)
				{
					return replacement;
				}
				else
				{
					System.err.println("No replacement found for index " + (pkmn&0xFF));
					return (byte) (rand.nextInt(251)+1);
				}
			case Mew:
				return (byte) 151;
			default:
				System.err.println("Unhandled randomisation mode");
				return (byte) (rand.nextInt(251)+1);
		}
	}
	
	
	private void setStarter(int offset, byte replacement){
		rom[offset] = replacement;
		rom[offset+2] = replacement;
		rom[offset+25] = replacement;
		rom[offset+36] = replacement;
	}
	
	private int randomiseLandArea(int offset){
		offset += 2; //area code
		offset += 3; //encounter rates
		for(int i=0; i<21; i++){
			//rom[offset] - level
			rom[offset+1] = getReplacement(rom[offset+1]);
			offset += 2;
		}
		return offset;
	}
	
	private int randomiseWaterArea(int offset){
		offset += 2; //area code
		offset += 1; //encounter rate
		for(int i=0; i<3; i++){
			//rom[offset] - level
			rom[offset+1] = getReplacement(rom[offset+1]);
			offset += 2;
		}
		return offset;
	}
	
	private int randomiseHBList(int offset){
		while(rom[offset]!=(byte)(0xFF)){
			//rom[offset] - chance (%)
			rom[offset+1] = getReplacement(rom[offset+1]);
			//rom[offset+2] - level
			offset += 3;
		}
		return offset+1;
	}
	
	private void randomiseFishingAreas(int offset){
		for(int i=offset; i<offset+FishingLength1; i+=3){
			if(rom[i+1] != 0) // index of 0 seems to have a special meaning here
			{
				//rom[i] - chance (cumulative)
				rom[i+1] = getReplacement(rom[i+1]);
				//rom[i+2] - level
			}
		}
		for(int i=offset+FishingLength1; i<offset+FishingLength1+FishingLength2; i+=2){
			rom[i] = getReplacement(rom[i]);
			//rom[i+1] - level
		}
	}
	
	private int randomiseTrainer(int offset){
		
		if(rom[offset] != (byte)80){
			logTrainerFormatError(offset);
			return offset+1;
		}
		
		if(rom[offset+1] == (byte)0){
			int i=0;
			for(; i<6; i++){
				if(rom[offset+2+2*i] == (byte)255)
					break;
				//rom[offset+2+2*i] - level
				rom[offset+3+2*i] = getReplacement(rom[offset+3+2*i]);
			}
			
			if(rom[offset+2+2*i] != (byte)255 || i == 0){
				logTrainerFormatError(offset);
			}
			offset = offset+2+2*i + 1;
		}
		else if(rom[offset+1] == (byte)1){
			int i=0;
			for(; i<6; i++){
				if(rom[offset+2+6*i] == (byte)255)
					break;
				//rom[offset+2+6*i] - level
				rom[offset+3+6*i] = getReplacement(rom[offset+3+6*i]);
				if(movesets == movesetsMode.Random){
					for(int j=1; j<=4; j++)
						rom[offset+3+6*i+j] = (byte)(rand.nextInt(251)+1);
				}
				
			}
			
			if(rom[offset+2+6*i] != (byte)255 || i == 0){
				logTrainerFormatError(offset);
			}
			offset = offset+2+6*i + 1;
		}
		else if(rom[offset+1] == (byte)2){
			int i=0;
			for(; i<6; i++){
				if(rom[offset+2+3*i] == (byte)255)
					break;
				//rom[offset+2+3*i] - level
				rom[offset+3+3*i] = getReplacement(rom[offset+3+3*i]);
				//rom[offset+4+3*i] - item
			}
			
			if(rom[offset+2+3*i] != (byte)255 || i == 0){
				logTrainerFormatError(offset);
			}
			offset = offset+2+3*i + 1;
		}
		else if(rom[offset+1] == (byte)3){
			int i=0;
			for(; i<6; i++){
				if(rom[offset+2+7*i] == (byte)255)
					break;
				//rom[offset+2+7*i] - level
				rom[offset+3+7*i] = getReplacement(rom[offset+3+7*i]);
				//rom[offset+4+7*i] - item
				if(movesets == movesetsMode.Random){
					for(int j=1; j<=4; j++)
						rom[offset+4+7*i+j] = (byte)(rand.nextInt(251)+1);
				}
			}
			
			if(rom[offset+2+7*i] != (byte)255 || i == 0){
				logTrainerFormatError(offset);
			}
			offset = offset+2+7*i + 1;
		}
		else{
			logTrainerFormatError(offset);
			return offset+1;
		}
		
		return offset;
	}
	
	private void logTrainerFormatError(int offset){
		System.err.println(String.format("Attempted to randomise a trainer at offset %x", offset));
		for(int i=offset; i<offset+32; i++){
			System.err.print(String.format("%x ", rom[i]));
		}
		System.err.println();
	}

	@Override
	public String[] currentStarters(){
		if(rom != null){
			int offset = game==version.Crystal ? CStartersOffset : GSStartersOffset;
			String[] ret = {indexToName[rom[offset]&0xFF],indexToName[rom[offset+66]&0xFF],indexToName[rom[offset+126]&0xFF]};
			return ret;
		}
		return null;
	}
}
