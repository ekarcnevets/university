USAGE:
================================================================================================================================

java TallestStack [boxFilePath]


GIVEN THIS INPUT:
================================================================================================================================
50 100 20
60 90 40
80 5 70


PROGRAM HAS OUTPUT:
================================================================================================================================

(2,2) (2,3) (1,3) 
The tallest stack possible has height '230'.


OUTPUT INTERPRETATION:
================================================================================================================================

The optimum height stack is output bottom to top as a space separated string of tuples, where (a,b) indicates box with index a, in orientation b.

Indexes begin from 1 and correspond to the line number in the file from which the boxes are read.

Orientations are in the range [1-3].


ORIENTATIONS:
================================================================================================================================

Orientations 1-3 defined as follows:
If we let H0, W0, D0 be the H, W, D given by the box file;

#1: H1=H0, W1=min(W0, D0), D1=max(W0, D0)
#2: H2=W1, W2=max(H1, D1), D2=min(H1, D1)
#3: H3=D1, W2=max(H1, W1), D3=min(H1, W1)