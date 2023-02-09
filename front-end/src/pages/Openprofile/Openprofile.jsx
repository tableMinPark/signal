import React, { useState, useEffect } from 'react'
import styled from '@emotion/styled'
// import JavaScript from '../../assets/image/Skilltest'
// import PostingCardItem from 'components/Posting/PostingCardItem'
import Box from '@mui/material/Box'
import '../../assets/styles/posting.css'
import Paging from 'components/Paging'
import api from 'api/Api'
import Openprofilecard from 'components/Openprofile/Openprofilecard'
// import SkillList from 'components/Apply/SkillList'
// import { useQuery } from 'react-query'
// import { Input } from 'assets/styles/apply'
// const SERVER_URL = 'http://tableminpark.iptime.org:8080/posting'

function Openprofile() {
  const [openList, setOpenList] = useState([])
  // result.data && setPostingList(result.data?.body?.postingList)
  // 테이블 코드 state Field 코드

  // 버튼 색 변경
  // console.log(...skillBtnList)

  const [page, setPage] = useState(1)
  const [size] = useState(20)
  const [count] = useState(0)
  const handleToPage = (page) => {
    setPage(page)
  }

  const openProfileList = async () => {
    await api.get(process.env.REACT_APP_API_URL + `/openprofile?page=${page}&size=${size}`).then((res) => {
      setOpenList(res.data.body.openProfileList)
      console.log(openList)
      console.log(JSON.stringify(res.data.body.openProfileList))
    })
  }

  const btnClickAxios = async () => {
    const res = await api.get(process.env.REACT_APP_API_URL + `/openprofile?page=${page}&size=${size}`)
    setOpenList(res.data.body.openProfileList)

    // console.log(Title)/
  }
  useEffect(() => {
    openProfileList()
  }, [])
  useEffect(() => {
    btnClickAxios()
  }, [page])

  return (
    <div>
      <Banner />
      <Container>
        <Box sx={{ width: '100%', mb: 2 }}></Box>

        {/* <Box sx={{ display: 'flex' }}>
          <Autocomplete
            multiple
            limitTags={3}
            size="small"
            id="multiple-limit-tags"
            options={Skilldata}
            getOptionLabel={(option) => option.name}
            onChange={(event, newValue) => {
              // console.log(newValue)
              handleChangeSkill(newValue)
            }}
            renderInput={(params) => <TextField {...params} label="기술 스택 검색" placeholder="Skill" />}
            sx={{ skillStyle, width: 1 / 3, mb: 3, backgroundColor: '#fbfbfd' }}
          />
          <div style={{ width: '50%' }}>dd</div>
        </Box> */}

        <Box sx={{ display: 'flex', flexDirection: 'row-reverse', marginBottom: '1em' }}>
          <button
            className="post-button"
            onClick={() => {
              console.log('버튼누름')
            }}
          >
            공고등록
          </button>
        </Box>
        <OpenCardList>
          {openList.map((open, i) => (
            <Openprofilecard open={open} key={i} />
          ))}
        </OpenCardList>
        <Paging page={page} count={count} setPage={handleToPage} size={size}></Paging>
      </Container>
    </div>
  )
}

const Container = styled.div`
  width: 80%;
  margin: auto;
  padding: 30px;
  border: 1px solid #574B9F;
  border-radius: 4px;
  flex-direction: column; 
  }
`
const Banner = styled.div`
  width: 100%;
  height: 300px;
  background: linear-gradient(89.98deg, rgba(255, 255, 255, 0) 0.02%, #bcb7d9 99.99%);
  border-radius: 0px;
`

const OpenCardList = styled.div`
  display: flex;
  flex-wrap: wrap;
  padding: 4px;
  gap: 10px;
  justify-content: center;
  &:hover {
    $ .postcard {
      background: cornflowerblue;
      color: white;
      transition: 0.5s;
    }
  }
`

export default Openprofile
