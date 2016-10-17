#/bin/bash
function build_file
{
    first=1
    if [ ! -z "$2" ]; then
        sed -f $2 $1 > temp.out
    else
        cp $1 temp.out
    fi
    while read line
    do
        if [ $first == 1 ]; then
            first=2
            str="$str$line"
        else
            str="$str""$line"
        fi
    done < temp.out
#    rm temp.out
    echo $str
}
function wrap
{
    echo "db.$1.insert({docid:\"$2\",\"persons\":$3});"
}
people=`build_file people-in-letters.json`
echo db.people.remove\({docid:\"english/harpur/letters\"}\)\;>upload.js
echo `wrap people english/harpur/letters "$people"`>>upload.js
