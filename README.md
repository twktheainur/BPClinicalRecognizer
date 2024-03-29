## Modified Stemmer for French Clinical text

We start from the snowball code of the default french stemmer and add the following rules at the end of the standard suffix rules (see code below): 

​     //Medical extensions           

​            'pulmonaire'

​               (<- 'poumon')

​            'cardiaque' 'cardi' 'cardiaqu' 'cardiaqUe' 'cardiaqU'

​               (<- 'coeur')



## Compiling modifications in the custom French clinical stemmer with Snowball

1. Follow the instructions here: http://snowball.tartarus.org/runtime/use.htm 

```shell
wget http://snowball.tartarus.org/dist/snowball_code.tgz
tar -xzf snowball_code.tgz
cd snowball_code
gcc -O -o snowball compiler/*.c
```

2. Create a frenchclinstem.sbt file with the following content and modify/extend/improve it as needed:

   ```Snowball
   routines (
              prelude postlude mark_regions
              RV R1 R2
              standard_suffix
              i_verb_suffix
              verb_suffix
              residual_suffix
              un_double
              un_accent
   )

   externals ( stem )

   integers ( pV p1 p2 )

   groupings ( v keep_with_s )

   stringescapes {}

   /* special characters (in ISO Latin I) */

   stringdef a^   hex 'E2'  // a-circumflex
   stringdef a`   hex 'E0'  // a-grave
   stringdef c,   hex 'E7'  // c-cedilla

   stringdef e"   hex 'EB'  // e-diaeresis (rare)
   stringdef e'   hex 'E9'  // e-acute
   stringdef e^   hex 'EA'  // e-circumflex
   stringdef e`   hex 'E8'  // e-grave
   stringdef i"   hex 'EF'  // i-diaeresis
   stringdef i^   hex 'EE'  // i-circumflex
   stringdef o^   hex 'F4'  // o-circumflex
   stringdef u^   hex 'FB'  // u-circumflex
   stringdef u`   hex 'F9'  // u-grave

   define v 'aeiouy{a^}{a`}{e"}{e'}{e^}{e`}{i"}{i^}{o^}{u^}{u`}'

   define prelude as repeat goto (

       (  v [ ('u' ] v <- 'U') or
              ('i' ] v <- 'I') or
              ('y' ] <- 'Y')
       )
       or
       (  ['y'] v <- 'Y' )
       or
       (  'q' ['u'] <- 'U' )
   )

   define mark_regions as (

       $pV = limit
       $p1 = limit
       $p2 = limit  // defaults

       do (
           ( v v next )
           or
           among ( // this exception list begun Nov 2006
               'par'  // paris, parie, pari
               'col'  // colis
               'tap'  // tapis
               // extensions possible here
           )
           or
           ( next gopast v )
           setmark pV
       )
       do (
           gopast v gopast non-v setmark p1
           gopast v gopast non-v setmark p2
       )
   )

   define postlude as repeat (

       [substring] among(
           'I' (<- 'i')
           'U' (<- 'u')
           'Y' (<- 'y')
           ''  (next)
       )
   )

   backwardmode (

       define RV as $pV <= cursor
       define R1 as $p1 <= cursor
       define R2 as $p2 <= cursor

       define standard_suffix as (
           [substring] among(

               'ance' 'iqUe' 'isme' 'able' 'iste' 'eux'
               'ances' 'iqUes' 'ismes' 'ables' 'istes'
                  ( R2 delete )
               'atrice' 'ateur' 'ation'
               'atrices' 'ateurs' 'ations'
                  ( R2 delete
                    try ( ['ic'] (R2 delete) or <-'iqU' )
                  )
               'logie'
               'logies'
                  ( R2 <- 'log' )
               'usion' 'ution'
               'usions' 'utions'
                  ( R2 <- 'u' )
               'ence'
               'ences'
                  ( R2 <- 'ent' )
               'ement'
               'ements'
               (
                   RV delete
                   try (
                       [substring] among(
                           'iv'   (R2 delete ['at'] R2 delete)
                           'eus'  ((R2 delete) or (R1<-'eux'))
                           'abl' 'iqU'
                                  (R2 delete)
                           'i{e`}r' 'I{e`}r'      //)
                                  (RV <-'i')      //)--new 2 Sept 02
                       )
                   )
               )
               'it{e'}'
               'it{e'}s'
               (
                   R2 delete
                   try (
                       [substring] among(
                           'abil' ((R2 delete) or <-'abl')
                           'ic'   ((R2 delete) or <-'iqU')
                           'iv'   (R2 delete)
                       )
                   )
               )
               'if' 'ive'
               'ifs' 'ives'
               (
                   R2 delete
                   try ( ['at'] R2 delete ['ic'] (R2 delete) or <-'iqU' )
               )
               'eaux' (<- 'eau')
               'aux'  (R1 <- 'al')
               'euse'
               'euses'((R2 delete) or (R1<-'eux'))

               'issement'
               'issements'(R1 non-v delete) // verbal

               // fail(...) below forces entry to verb_suffix. -ment typically
               // follows the p.p., e.g 'confus{e'}ment'.

               'amment'   (RV fail(<- 'ant'))
               'emment'   (RV fail(<- 'ent'))
               'ment'
               'ments'    (test(v RV) fail(delete))
                          // v is e,i,u,{e'},I or U
               //Medical extensions           
               'pulmonaire'
                  (<- 'poumon')
               'cardiaque' 'cardi' 'cardiaqu' 'cardiaqUe' 'cardiaqU'
                  (<- 'coeur')
           )
       )

       define i_verb_suffix as setlimit tomark pV for (
           [substring] among (
               '{i^}mes' '{i^}t' '{i^}tes' 'i' 'ie' 'ies' 'ir' 'ira' 'irai'
               'iraIent' 'irais' 'irait' 'iras' 'irent' 'irez' 'iriez'
               'irions' 'irons' 'iront' 'is' 'issaIent' 'issais' 'issait'
               'issant' 'issante' 'issantes' 'issants' 'isse' 'issent' 'isses'
               'issez' 'issiez' 'issions' 'issons' 'it'
                   (non-v delete)
           )
       )

       define verb_suffix as setlimit tomark pV for (
           [substring] among (
               'ions'
                   (R2 delete)

               '{e'}' '{e'}e' '{e'}es' '{e'}s' '{e`}rent' 'er' 'era' 'erai'
               'eraIent' 'erais' 'erait' 'eras' 'erez' 'eriez' 'erions'
               'erons' 'eront' 'ez' 'iez'

               // 'ons' //-best omitted

                   (delete)

               '{a^}mes' '{a^}t' '{a^}tes' 'a' 'ai' 'aIent' 'ais' 'ait' 'ant'
               'ante' 'antes' 'ants' 'as' 'asse' 'assent' 'asses' 'assiez'
               'assions'
                   (delete
                    try(['e'] delete)
                   )
           )
       )

       define keep_with_s 'aiou{e`}s'

       define residual_suffix as (
           try(['s'] test non-keep_with_s delete)
           setlimit tomark pV for (
               [substring] among(
                   'ion'           (R2 's' or 't' delete)
                   'ier' 'i{e`}re'
                   'Ier' 'I{e`}re' (<-'i')
                   'e'             (delete)
                   '{e"}'          ('gu' delete)
               )
           )
       )

       define un_double as (
           test among('enn' 'onn' 'ett' 'ell' 'eill') [next] delete
       )

       define un_accent as (
           atleast 1 non-v
           [ '{e'}' or '{e`}' ] <-'e'
       )
   )

   define stem as (

       do prelude
       do mark_regions
       backwards (

           do (
               (
                    ( standard_suffix or
                      i_verb_suffix or
                      verb_suffix
                    )
                    and
                    try( [ ('Y'   ] <- 'i' ) or
                           ('{c,}'] <- 'c' )
                    )
               ) or
               residual_suffix
           )

           // try(['ent'] RV delete) // is best omitted

           do un_double
           do un_accent
       )
       do postlude
   )
   ```

   ​

3. Compile the snowball source into a java class:

   ```Shell
   ./snowball frenchclinstem.sbt -o FrenchClinicalStemmer -n FrenchClinicalStemmer -p org.tartarus.snowball.SnowballStemmer -P org.sifrproject.stemming -j
   ```

   ​

4. Replace FrenchClinicalStemmer in org.sifrproject.stemming by the new FrenchClinicalStemmer.java