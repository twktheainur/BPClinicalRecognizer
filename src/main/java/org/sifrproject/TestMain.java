package org.sifrproject;

import org.sifrproject.recognizer.AnnotationToken;
import org.sifrproject.recognizer.ConceptRecognizer;
import org.sifrproject.recognizer.FaironConceptRecognizer;

import java.nio.file.Paths;
import java.util.List;

public class TestMain {
    public static void main(String... args) {
        ConceptRecognizer recognizer = new FaironConceptRecognizer(Paths.get(args[0]));
        long startTime = System.currentTimeMillis();
        List<AnnotationToken> tokens = recognizer.recognize("PEC SMUR  à domicile pour dyspnée et douleur thoracique....patient de 80+ ans en bon EG, présentant depuis 7 jours une toux grasse ne cédant pas sous ttt symptomatique (pas d'ATB) et aerosols. Pas de fièvre d'après le patient. Notion de douleur thoracique fluctuantes, atypiques à type de pincement médiothoracique haut, dont le patient à l'habitude. Ce sont les mêmes douleurs que d'habitude, bilan réalisé par XXXX au sujet de ces douleurs : douleurs pariétales....A notre arrivée : sat 80%aa, bonne HD, pas de fièvre. Ictère cutanéomuqueux assez franc, paleur, pas de marbrures....- neuro : G15, cohérent, orienté, pas de signe de focalisation..- pulm : discret tirage sus claviculaire, toux grasse non expectorée, pas de signes d'hypercapnie clinique. Ronchi diffus à l'auscultation pas de foyer franc..- cardio : BDC reguliers, PM, pas d'OMI, mollets souples, indolores, pouls periph difficilement retrouvés aux MI, TA symétriques, pas de signes d'IVD..- abdo : ictère, pas d'ascite, abdo souple et indolore, pas d'hsm évidente sous réserve de l'examen (dans VSAV et habillé)....ECG : parfois EE, sur QRS natifs, sous décalage ST en v4v5v6, onde T negatives en inférieur et postérieur..hémocue 12.3, nous récupérons une NFS du 2/1/17 à 9.3....a eu aerosol B+A pendant transport avec amélioration ressentie par patient....patient non vacciné pour la grippe..vit à domicile avec son épouse, sont autonomes. Se déplace avec canne. Fils = aidant naturel....un passage récent aux urgences pour rupture de varice le 13/08 (simple consultation)patiente de XX ans [90+] adressée par MDR XXXX pour détresse respiratoire, désaturation..GIR 5 en MDR....à l'examen : constantes ok mais HTA....CARDIO :bdc réguliers sans souffle, pas de DT, pas de signes de TVP, OMI modérés, pas de marbrures, pouls periph percus..PULM : MV+/+ crépitants bilat, qqs sibilants,dyspnée de repos, pas de toux, pas de cyanose..ABDO : pléthorique, souple, dépressible, indolore, pas de défense ni contracture, pas de nausées ni vomissement..NEURO : consciente, orientée, pas de déficit neuro focal, pas de céphalées....CAT : Lasilix, Risordan, VNI....Appel Médecin traitant (Dr XXXX XXXXX) : ..prise de sang datant de Fevrier 2003  : GB à 5900, NFS normale, GGT 153, clairance créat 41, pas de problème hémato sous jacent connu..me dit que la patiente a eu un pacemaker en aout 2012 pour vertiges, rétrécissement aortique sévère, rétrécissement mitral, souffle aortique à 3/6, et qu'elle aurait fait il y a 15 jours une suspicion d'OAP...");
        System.out.println(String.format("Time: %d", System.currentTimeMillis() - startTime));
        for (AnnotationToken annotationToken : tokens) {
            System.out.println(annotationToken);
        }
    }
}
