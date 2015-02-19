package com.traviswheeler.ninja;

import com.traviswheeler.ninja.SequenceFileReader.AlphabetType;


public class DistanceCalculator {
	
	public static enum CorrectionType {not_assigned, none, JukesCantor/*DNA*/, Kimura2/*DNA*/, FastTree /*amino*/};
	
	AlphabetType   alph_type;
	CorrectionType corr_type;
	
	char[] dna_chars = {'A','G','C','T'}; 
	char[] aa_chars = {'A','R','N','D','C','Q','E','G','H','I','L','K','M','F','P','S','T','W','Y','V'}; 
	int[] inv_alph;
	char[][] A;
	
	public DistanceCalculator (char[][] A /*alignment*/, AlphabetType alphType, CorrectionType corrType ) {
		this.A  = A;
		this.corr_type = corrType;
		this.alph_type = alphType;

		if (corr_type == CorrectionType.not_assigned) {
			if (alph_type == AlphabetType.amino) {
				corr_type = CorrectionType.FastTree;
			} else {
				corr_type = CorrectionType.Kimura2;
			}
		}
		
		inv_alph = getInverseAlphabet( alph_type==AlphabetType.dna ? dna_chars : aa_chars );
	}

		
	double calc (int a, int b) throws Exception {

		float dist = 0.0f;
		float maxscore =  (corr_type == CorrectionType.none ? 1.0f : 3.0f);
		

		
		if (alph_type == AlphabetType.amino) {
			if (corr_type != CorrectionType.none && corr_type != CorrectionType.FastTree)
				throw new GenericNinjaException("illegal choice of correction method; must be 'n' or 's'");

			
			float[][] matrix = getBLOSUM45();
			int count = 0;
			for (int i=0; i<A[0].length; i++){
				if (inv_alph[A[a][i]] >= 0 && inv_alph[A[b][i]] >= 0) { // both are characters in the core alphabet
					dist += matrix[ inv_alph[A[a][i]]  ]  [ inv_alph[A[b][i]]  ] ;
					count++;
				}
			}
			if (count==0) {
				dist = maxscore;
			} else {
				dist /= count;
				if (corr_type == CorrectionType.FastTree)
					dist = dist < 0.91 ? (float)(-1.3*Math.log(1.0 - dist)) : maxscore;
			}
		} else {
			if (corr_type == CorrectionType.FastTree)
				throw new GenericNinjaException("illegal choice of correction method; must be 'n', 'j', or 'k'");

			
			int p=0; //transitions
			int q=0; //transversion
			int count = 0;
			for (int i=0; i<A[0].length; i++) {
				if (inv_alph[A[a][i]] >= 0 && inv_alph[A[b][i]] >= 0) { // both are characters in the core alphabet
					count++;
					if (inv_alph[A[a][i]] != inv_alph[A[b][i]]) {
						//count transitions (A-G, C-T) and transversions (others)
						if ( (inv_alph[A[a][i]] <2 && inv_alph[A[b][i]] < 2) || // both A and G, and not equal 
								(inv_alph[A[a][i]] >1 && inv_alph[A[b][i]] > 1) ) // both C and T, not equal
							p++;
						else
							q++;
					}
				}	
			}
			
			if (count == 0) {
					dist = maxscore;
			} else {
				float p_f = (float)p / count;
				float q_f = (float)q / count;
				
				if ( p_f+q_f == 0) 
					dist = 0;
				else if (corr_type == CorrectionType.JukesCantor)
					dist = (float)(-(0.75)*Math.log(1.0-(4.0/3.0)*(p_f+q_f)));  
				else if (corr_type == CorrectionType.Kimura2)
					dist = (float)(-0.5 * Math.log(1.0 - 2*p_f - q_f) - 0.25 * Math.log( 1.0-2*q_f ));
				else if (corr_type == CorrectionType.none)
					dist = p_f + q_f;
			}
			
		}	
		
		double dist_d = (dist < maxscore ? dist : maxscore); // I do this because the java float keeps enough precision for my needs, but
															//for reasons I don't understand, uses different such values for division/multiplication
		         											//(used in distance-file prinout) vs casting to a double.  This is a little faster
															//than just using doubles throughout.
		return dist_d;
	}
	
	
	int[] getInverseAlphabet (char[] alph) {
		int[] inv_alph = new int[256];
		for (int i=0; i<256; i++) 
			inv_alph[i]=-1;
		for (int i=0; i<alph.length; i++)
			inv_alph[alph[i]] = i;
		return inv_alph;
	}

	
	float[][] getBLOSUM45 () {
		float[][] bl45 =
		{
	      {0f, 1.31097856f, 1.06573001f, 1.26827829f, 0.90471293f, 1.05855446f, 1.05232790f, 0.76957444f, 1.27579668f, 0.96460409f, 0.98717819f, 1.05007594f, 1.05464162f, 1.19859874f, 0.96740447f, 0.70049019f, 0.88006018f, 1.09748548f, 1.28141710f, 0.80003850f},
	      {1.31097856f, 0f, 0.80108902f, 0.95334071f, 1.36011107f, 0.63154377f, 0.79101490f, 1.15694899f, 0.76115257f, 1.45014917f, 1.17792001f, 0.39466107f, 0.99880755f, 1.13514340f, 1.15432562f, 1.05309036f, 1.05010474f, 1.03938321f, 0.96321690f, 1.20274751f},
	      {1.06573001f, 0.80108902f, 0f, 0.48821721f, 1.10567116f, 0.81497020f, 0.81017644f, 0.74648741f, 0.61876156f, 1.17886558f, 1.52003670f, 0.80844267f, 1.28890258f, 1.16264109f, 1.18228799f, 0.67947568f, 0.85365861f, 1.68988558f, 1.24297493f, 1.55207513f},
	      {1.26827829f, 0.95334071f, 0.48821721f, 0f, 1.31581050f, 0.76977847f, 0.48207762f, 0.88836175f, 0.73636084f, 1.76756333f, 1.43574761f, 0.76361291f, 1.53386612f, 1.74323672f, 0.88634740f, 0.80861404f, 1.01590147f, 1.59617804f, 1.17404948f, 1.46600946f},
	      {0.90471293f, 1.36011107f, 1.10567116f, 1.31581050f, 0f, 1.38367893f, 1.37553994f, 1.26740695f, 1.32361065f, 1.26087264f, 1.02417540f, 1.37259631f, 1.09416720f, 0.98698208f, 1.59321190f, 0.91563878f, 0.91304285f, 1.80744143f, 1.32944171f, 0.83002214f},
	      {1.05855446f, 0.63154377f, 0.81497020f, 0.76977847f, 1.38367893f, 0f, 0.50694279f, 1.17699648f, 0.61459544f, 1.17092829f, 1.19833088f, 0.63734107f, 0.80649084f, 1.83315144f, 0.93206447f, 0.85032169f, 1.06830084f, 1.05739353f, 0.97990742f, 1.54162503f},
	      {1.05232790f, 0.79101490f, 0.81017644f, 0.48207762f, 1.37553994f, 0.50694279f, 0f, 1.17007322f, 0.76978695f, 1.46659942f, 1.19128214f, 0.63359215f, 1.27269395f, 1.44641491f, 0.73542857f, 0.84531998f, 1.06201695f, 1.32439599f, 1.22734387f, 1.53255698f},
	      {0.76957444f, 1.15694899f, 0.74648741f, 0.88836175f, 1.26740695f, 1.17699648f, 1.17007322f, 0f, 1.12590070f, 1.70254155f, 1.38293205f, 1.16756929f, 1.17264582f, 1.33271035f, 1.07564768f, 0.77886828f, 1.23287107f, 0.96853965f, 1.42479529f, 1.41208067f},
	      {1.27579668f, 0.76115257f, 0.61876156f, 0.73636084f, 1.32361065f, 0.61459544f, 0.76978695f, 1.12590070f, 0f, 1.41123246f, 1.14630894f, 0.96779528f, 0.77147945f, 1.10468029f, 1.12334774f, 1.02482926f, 1.28754326f, 1.27439749f, 0.46868384f, 1.47469999f},
	      {0.96460409f, 1.45014917f, 1.17886558f, 1.76756333f, 1.26087264f, 1.17092829f, 1.46659942f, 1.70254155f, 1.41123246f, 0f, 0.43335051f, 1.46346092f, 0.46296554f, 0.66291968f, 1.07010201f, 1.23000200f, 0.97348545f, 0.96354620f, 0.70872476f, 0.35120011f},
	      {0.98717819f, 1.17792001f, 1.52003670f, 1.43574761f, 1.02417540f, 1.19833088f, 1.19128214f, 1.38293205f, 1.14630894f, 0.43335051f, 0f, 1.49770950f, 0.47380007f, 0.53847312f, 1.37979627f, 1.58597231f, 0.99626739f, 0.98609554f, 0.72531066f, 0.57054219f},
	      {1.05007594f, 0.39466107f, 0.80844267f, 0.76361291f, 1.37259631f, 0.63734107f, 0.63359215f, 1.16756929f, 0.96779528f, 1.46346092f, 1.49770950f, 0f, 1.00797618f, 1.44331961f, 0.92459908f, 1.06275728f, 1.05974425f, 1.04892430f, 0.97205882f, 1.21378822f},
	      {1.05464162f, 0.99880755f, 1.28890258f, 1.53386612f, 1.09416720f, 0.80649084f, 1.27269395f, 1.17264582f, 0.77147945f, 0.46296554f, 0.47380007f, 1.00797618f, 0f, 0.72479754f, 1.16998686f, 1.34481214f, 1.06435197f, 1.05348497f, 0.77487815f, 0.60953285f},
	      {1.19859874f, 1.13514340f, 1.16264109f, 1.74323672f, 0.98698208f, 1.83315144f, 1.44641491f, 1.33271035f, 1.10468029f, 0.66291968f, 0.53847312f, 1.44331961f, 0.72479754f, 0f, 1.32968844f, 1.21307373f, 0.96008757f, 0.47514255f, 0.34948536f, 0.69273324f},
	      {0.96740447f, 1.15432562f, 1.18228799f, 0.88634740f, 1.59321190f, 0.93206447f, 0.73542857f, 1.07564768f, 1.12334774f, 1.07010201f, 1.37979627f, 0.92459908f, 1.16998686f, 1.32968844f, 0f, 0.97908742f, 0.97631161f, 1.21751652f, 1.42156458f, 1.40887880f},
	      {0.70049019f, 1.05309036f, 0.67947568f, 0.80861404f, 0.91563878f, 0.85032169f, 0.84531998f, 0.77886828f, 1.02482926f, 1.23000200f, 1.58597231f, 1.06275728f, 1.34481214f, 1.21307373f, 0.97908742f, 0f, 0.56109848f, 1.76318885f, 1.29689226f, 1.02015839f},
	      {0.88006018f, 1.05010474f, 0.85365861f, 1.01590147f, 0.91304285f, 1.06830084f, 1.06201695f, 1.23287107f, 1.28754326f, 0.97348545f, 0.99626739f, 1.05974425f, 1.06435197f, 0.96008757f, 0.97631161f, 0.56109848f, 0f, 1.39547634f, 1.02642577f, 0.80740466f},
	      {1.09748548f, 1.03938321f, 1.68988558f, 1.59617804f, 1.80744143f, 1.05739353f, 1.32439599f, 0.96853965f, 1.27439749f, 0.96354620f, 0.98609554f, 1.04892430f, 1.05348497f, 0.47514255f, 1.21751652f, 1.76318885f, 1.39547634f, 0f, 0.32000293f, 1.26858915f},
	      {1.28141710f, 0.96321690f, 1.24297493f, 1.17404948f, 1.32944171f, 0.97990742f, 1.22734387f, 1.42479529f, 0.46868384f, 0.70872476f, 0.72531066f, 0.97205882f, 0.77487815f, 0.34948536f, 1.42156458f, 1.29689226f, 1.02642577f, 0.32000293f, 0f, 0.93309543f},
	      {0.80003850f, 1.20274751f, 1.55207513f, 1.46600946f, 0.83002214f, 1.54162503f, 1.53255698f, 1.41208067f, 1.47469999f, 0.35120011f, 0.57054219f, 1.21378822f, 0.60953285f, 0.69273324f, 1.40887880f, 1.02015839f, 0.80740466f, 1.26858915f, 0.93309543f, 0f}
		};
		
		return bl45;
	}
	
}


