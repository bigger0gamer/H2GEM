//This class just holds Strings for a single gametype, making holding these Strings in an ArrayList easy.
//The strings are public, so this class has no methods other than a couple constructors.
class H2GT
{
  //gametypeFolder contains the name of the folder holding the gametype. Ex. S0000000 or 123456789ABC
  
  //gametypeGamemode contains the type of gamemode
  //Slayer = "slayer"
  //Capture the Flag = "ctf"
  //Oddball = "oddball"
  //King of the Hill = "koth"
  //Juggernaut = "juggernaut"
  //Territories = "territories"
  //Assault = "assault"
  
  //gametypeName holds the name of the gametype. Ex. SWAT or Zombies
  public String gametypeFolder;
  public String gametypeGamemode;
  public String gametypeName;
  
  //a constructor with no arguments that makes all the Strings emtpy
  public H2GT()
  {
    gametypeFolder = "";
    gametypeGamemode = "";
    gametypeName = "";
  }
  
  //a constructor that takes 3 strings and assigns them to the global variables
  public H2GT(String gtF, String gtGm, String gtN)
  {
    gametypeFolder = gtF;
    gametypeGamemode = gtGm;
    gametypeName = gtN;
  }
}